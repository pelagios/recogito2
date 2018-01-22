require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/map/map',
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config',
  'document/map/mapPopup'], function(Map, Formatting, AnnotationUtils, PlaceUtils, API, Config, MapPopup) {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4,

      FILL_COLOR = '#31a354',

      STROKE_COLOR = '#006d2c',

      POINT_STYLE = {
        color       : STROKE_COLOR,
        fillColor   : FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 1
      },

      SHAPE_STYLE = jQuery.extend({}, POINT_STYLE, { fillOpacity: 0.5 }),

      TOUCH_DISTANCE_THRESHOLD = 18;

  jQuery(document).ready(function() {

    var map = new Map(jQuery('.map')),

        pointMarkerLayer = L.layerGroup(),

        regionMarkerLayer = L.layerGroup(),

        /** Lookup table { gazetteerUri -> [ annotation] } **/
        annotationsByGazetteerURI = {},

        markerScaleFn,

        getAnnotationsForPlace = function(place) {
          var uris = PlaceUtils.getURIs(place),
              annotations = [];

          jQuery.each(uris, function(idx, uri) {
            var annotationsForURI = annotationsByGazetteerURI[uri];
            if (annotationsForURI)
              annotations = annotations.concat(annotationsForURI);
          });
          return annotations;
        },

        onAnnotationsLoaded = function(annotations) {
          // Loop through all place bodies of all annotations
          jQuery.each(annotations, function(i, annotation) {
            jQuery.each(AnnotationUtils.getBodiesOfType(annotation, 'PLACE'), function(j, placeBody) {
              if (placeBody.reference) {
                var annotationsAtPlace = annotationsByGazetteerURI[placeBody.reference.uri];
                if (annotationsAtPlace)
                  annotationsAtPlace.push(annotation);
                else
                  annotationsByGazetteerURI[placeBody.reference.uri] = [ annotation ];
              }
            });
          });

          computeMarkerScaleFn();

          // After the annotations are loaded, load the places
          return API.listPlacesInDocument(Config.documentId, 0, 2000);
        },

        onPlacesLoaded = function(response) {

          var createPointMarker = function(place) {
                // The epic struggle between Leaflet vs. GeoJSON
                var latlng = [place.representative_point[1], place.representative_point[0]],
                    markerSize = markerScaleFn(getAnnotationsForPlace(place).length),
                    style = jQuery.extend({}, POINT_STYLE, { radius: markerSize });

                return L.circleMarker(latlng, style).addTo(pointMarkerLayer);
              },

              createShapeMarker = function(place) {
                return L.geoJson(place.representative_geometry, SHAPE_STYLE).addTo(regionMarkerLayer);
              };

          jQuery.each(response.items, function(idx, place) {
            var annotations = getAnnotationsForPlace(place),
                marker;

            if ((place.representative_point || place.representative_geometry) && annotations.length > 0) {
              marker = (place.representative_geometry && place.representative_geometry.type !== 'Point') ?
                createShapeMarker(place) : createPointMarker(place);
              marker.place = place; // TODO Hack! Clean this up
              marker.on('click', function() {
                var popup = new MapPopup(marker, place, getAnnotationsForPlace(place));
                map.add(popup);
              });
            }
          });

          pointMarkerLayer.getLayers().forEach(function(layer) {
            layer.bringToFront();
          });
        },

        computeMarkerScaleFn = function() {
          var min = 9007199254740991, max = 1,
              k, d, avg;

          // Determine min/max annotations per place
          jQuery.each(annotationsByGazetteerURI, function(uri, annotations) {
            var count = annotations.length;
            if (count < min)
              min = count;
            if (count > max)
              max = count;
          });

          if (min === max) {
            // All places are equal (or just one place) - use min marker size
            markerScaleFn = function(noOfAnnotations) { return MIN_MARKER_SIZE; };
          } else {
            // Marker size y = fn(no_of_annotations) is linear fn according to y = k * x + d
            k = (MAX_MARKER_SIZE - MIN_MARKER_SIZE) / (max - min);
            d = ((MIN_MARKER_SIZE * max) - (MAX_MARKER_SIZE * min)) / (max - min);
            markerScaleFn = function(noOfAnnotations) { return k * noOfAnnotations + d; };
         }
        },

        onLoadError = function(error) {
          // TODO implement
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
          var xy = map.leafletMap.latLngToContainerPoint(latlng),
              nearest = { distSq: 9007199254740992 }, // Distance to nearest initialied with Integer.MAX
              nearestXY, distPx,

              // TODO need a good strategy to deal with point vs. region markers
              markers = pointMarkerLayer.getLayers().concat(regionMarkerLayer.getLayers());

          markers.forEach(function (marker) {
            var markerLatLng = marker.getBounds().getCenter(),
                distSq =
                  Math.pow(latlng.lat - markerLatLng.lat, 2) +
                  Math.pow(latlng.lng - markerLatLng.lng, 2);

            if (distSq < nearest.distSq)
              nearest = { marker: marker, latlng: markerLatLng, distSq: distSq };
          });

          if (nearest.marker) {
            nearestXY = map.leafletMap.latLngToContainerPoint(nearest.latlng);
            distPx =
              Math.sqrt(
                Math.pow((xy.x - nearestXY.x), 2) +
                Math.pow((xy.y - nearestXY.y), 2));

            if (distPx < maxDistance) {
              // TODO clean up or eliminate need for marker.place
              var popup = new MapPopup(nearest.latlng, nearest.marker.place, getAnnotationsForPlace(nearest.marker.place));
              map.add(popup);
            }
          }
        };

    map.add(regionMarkerLayer);
    map.add(pointMarkerLayer);

    map.leafletMap.on('click', function(e) {
      selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD);
    });

    API.listAnnotationsInDocument(Config.documentId)
       .then(onAnnotationsLoaded)
       .done(onPlacesLoaded)
       .fail(onLoadError);
  });

});
