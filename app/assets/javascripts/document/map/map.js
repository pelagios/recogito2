define([
  'common/map/basemap',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/map/layers/pointLayer',
  'document/map/layers/shapeLayer',
  'document/map/style/mapStyle',
  'document/map/mapPopup'
], function(
  BaseMap,
  AnnotationUtils,
  PlaceUtils,
  PointLayer,
  ShapeLayer,
  MapStyle,
  MapPopup
) {

  var TOUCH_DISTANCE_THRESHOLD = 18;

  var Map = function(element) {
    // Inherit from basemap right here, so that point/shapeLayer have access to .add method
    BaseMap.apply(this, [ element ]);

    var self = this,

        mapStyle = new MapStyle(),

        pointLayer = new PointLayer(this, mapStyle),

        shapeLayer = new ShapeLayer(this, mapStyle),

        /** Lookup table { gazetteerUri -> [ annotation] } **/
        annotationsByGazetteerURI,

        setAnnotations = function(annotationView) {
          var buildLookupTable = function(annotations) {
                  annotationsByGazetteerURI = {};

                  // For each annotation...
                  annotations.forEach(function(annotation) {

                    // ...loop through each body...
                    AnnotationUtils.getBodiesOfType(annotation, 'PLACE').forEach(function(placeBody) {

                      // ...and populate the lookup table.
                      if (placeBody.uri) {
                        var annotationsAtPlace = annotationsByGazetteerURI[placeBody.uri];
                        if (annotationsAtPlace) annotationsAtPlace.push(annotation);
                        else annotationsByGazetteerURI[placeBody.uri] = [annotation];
                      }
                    });
                  });
                };

          buildLookupTable(annotationView.listAnnotations());

          shapeLayer.init(annotationsByGazetteerURI);
          pointLayer.init(annotationsByGazetteerURI);

          mapStyle.init(annotationView);
        },

        setPlaces = function(places) {
          var withGeometry = places.filter(function(place) {
                return place.representative_point || place.representative_geometry;
              }).sort(function(a, b) {
                // TODO is sorting by bbox size enough?
                var bboxSizeA = PlaceUtils.getBBoxSize(a),
                    bboxSizeB = PlaceUtils.getBBoxSize(b);
                return bboxSizeB - bboxSizeA;
              });

          withGeometry.forEach(function(place) {
            var annotations = getAnnotationsForPlace(place),
                marker;

            if (annotations.length > 0) {
              marker = (place.representative_geometry && place.representative_geometry.type !== 'Point') ?
                shapeLayer.addShape(place) : pointLayer.addMarker(place);

              marker.place = place;
              marker.on('click', function() {
                var popup = new MapPopup(marker, place, getAnnotationsForPlace(place));
                self.add(popup);
              });
            }
          });

          pointLayer.bringToFront();
        },

        redraw = function() {
          shapeLayer.redraw();
          pointLayer.redraw();
        },

        getAnnotationsForPlace = function(place) {
          var uris = PlaceUtils.getURIs(place),
              annotations = [];

          uris.forEach(function(uri) {
            var annotationsForURI = annotationsByGazetteerURI[uri];
            if (annotationsForURI)
              annotations = annotations.concat(annotationsForURI);
          });

          return annotations;
        },

        /**
         * Selects the marker nearest the given latlng. This is primarily a
         * means to support touch devices, where touch events will usually miss
         * the markers because they are too small for properly hitting them.
         *
         * TODO this could be heavily optimized by some sort of spatial indexing or bucketing,
         * but seems to work reasonably well even for lots of markers.
         *
         */
        selectNearest = function(latlng, maxDistance) {
          var xy = self.leafletMap.latLngToContainerPoint(latlng),
              nearest = { distSq: 9007199254740992 }, // Distance to nearest initialied with Integer.MAX
              nearestXY, distPx,

              // TODO need a good strategy to deal with point vs. region markers
              markers = pointLayer.getLayers().concat(shapeLayer.getLayers());

          markers.forEach(function (marker) {
            var markerLatLng = (marker.getLatLng) ?
                  marker.getLatLng() : // Points
                  marker.getBounds().getCenter(), // GeoJSON
                distSq =
                  Math.pow(latlng.lat - markerLatLng.lat, 2) +
                  Math.pow(latlng.lng - markerLatLng.lng, 2);

            if (distSq < nearest.distSq)
              nearest = { marker: marker, latlng: markerLatLng, distSq: distSq };
          });

          if (nearest.marker) {
            nearestXY = self.leafletMap.latLngToContainerPoint(nearest.latlng);
            distPx =
              Math.sqrt(
                Math.pow((xy.x - nearestXY.x), 2) +
                Math.pow((xy.y - nearestXY.y), 2));

            if (distPx < maxDistance) {
              var popup = new MapPopup(nearest.marker, nearest.marker.place, getAnnotationsForPlace(nearest.marker.place));
              self.add(popup);
            }
          }
        };

    mapStyle.on('change', redraw);

    this.getAnnotationsForPlace = getAnnotationsForPlace;
    this.setAnnotations = setAnnotations;
    this.setPlaces = setPlaces;

    this.leafletMap.on('click', function(e) {
      selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD);
    });
  };
  Map.prototype = Object.create(BaseMap.prototype);

  return Map;

});
