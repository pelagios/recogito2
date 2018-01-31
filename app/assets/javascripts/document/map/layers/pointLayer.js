define([], function() {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4;

  var PointLayer = function(map, mapStyle) {

    var pointLayer = L.layerGroup(),

        markerScaleFn,

        initMarkerScaleFn = function(annotationsByGazetteerURI) {
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

        bringToFront = function() {
          pointLayer.getLayers().forEach(function(layer) {
            layer.bringToFront();
          });
        },

        addMarker = function(place) {
          // The epic struggle between Leaflet vs. GeoJSON
          var latlng = [place.representative_point[1], place.representative_point[0]],
              annotationsForPlace = map.getAnnotationsForPlace(place),
              markerSize = markerScaleFn(annotationsForPlace.length),
              style = jQuery.extend({}, mapStyle.getPointStyle(place), { radius: markerSize });

          return L.circleMarker(latlng, style).addTo(pointLayer);
        },

        // TODO clean up - shouldn't expose this to the outside
        // TODO currently used by map.selectNearest
        getLayers = function() {
          return pointLayer.getLayers();
        };

    map.add(pointLayer);

    this.addMarker = addMarker;
    this.bringToFront = bringToFront;
    this.getLayers = getLayers;
    this.init = initMarkerScaleFn;
  };

  return PointLayer;

});
