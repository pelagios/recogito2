define([
  'common/config',
  'document/annotation/common/selection/abstractHighlighter'],

  function(Config, AbstractHighlighter) {

    var POINT_STYLE = new ol.style.Style({
          image: new ol.style.Circle({
            radius : 6,
            fill   : new ol.style.Fill({ color: '#4c4c4c', opacity: 0.6 }),
            stroke : new ol.style.Stroke({ color: '#262626', width: 1.5 })
          })
        }),

        POINT_STYLE_HI = new ol.style.Style({
          image: new ol.style.Circle({
            radius : 6,
            fill   : new ol.style.Fill({ color: '#ff4c4c', opacity: 0.6 }),
            stroke : new ol.style.Stroke({ color: '#262626', width: 1.5 })
          })
        }),

        MIN_SELECTION_DISTANCE = 10;

    var PointHighlighter = function(olMap) {

      var pointVectorSource = new ol.source.Vector({}),

          currentHighlight = false,

          selectPoint = function(coordinate) {
            var pointFeature = new ol.Feature({
                  'geometry': new ol.geom.Point(coordinate)
                });

            pointVectorSource.addFeature(pointFeature);
          },

          /**
           * Computes the distance (in pixel) between a screen (pixel) location, and
           * a coordinate on the map.
           */
          computePxDistance = function(px, coord) {
            var otherPx = olMap.getPixelFromCoordinate(coord),
                dx = px[0] - otherPx[0],
                dy = px[1] - otherPx[1];

            return Math.sqrt(dx * dx + dy * dy);
          },

          onMousemove = function(e) {
            if (!e.dragging) {
              var closestFeature = pointVectorSource.getClosestFeatureToCoordinate(e.coordinate),
                  closestPoint = (closestFeature) ? closestFeature.getGeometry().getClosestPoint(e.coordinate) : false;

              if (closestPoint && computePxDistance(e.pixel, closestPoint) < MIN_SELECTION_DISTANCE) {
                // Highlight the clostest feature, unless already highlighted
                if (currentHighlight !== closestFeature) {

                  // Un-highlight the previous highlight, if needed
                  if (currentHighlight)
                    currentHighlight.setStyle(POINT_STYLE);

                  currentHighlight = closestFeature;
                  closestFeature.setStyle(POINT_STYLE_HI);
                }
              } else if (currentHighlight) {
                // Clear the previous highlight, if any
                currentHighlight.setStyle(POINT_STYLE);
                currentHighlight = false;
              }
            }
          },

          getCurrentHighlight = function() {
            if (currentHighlight)
              return currentHighlight.getGeometry().getCoordinates();
          },

          refreshAnnotation = function(annotation) {
            // TODO implement
            console.log('refreshAnnotation', annotation);
          },

          convertSelectionToAnnotation = function(selection, annotationStub) {
            // TODO this currently assumes 'point:' anchors only!
            var anchor = annotationStub.anchor,
                x = parseInt(anchor.substring(anchor.indexOf(':') + 1, anchor.indexOf(','))),
                y = - parseInt(anchor.substring(anchor.indexOf(',') + 1));

            var pointFeature = new ol.Feature({
                  'geometry': new ol.geom.Point([ x, y ])
                });

            pointVectorSource.addFeature(pointFeature);
          };

      olMap.addLayer(new ol.layer.Vector({
        source: pointVectorSource,
        style: POINT_STYLE
      }));

      olMap.on('pointermove', onMousemove);

      this.getCurrentHighlight = getCurrentHighlight;
      this.refreshAnnotation = refreshAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;

      AbstractHighlighter.apply(this);
    };
    PointHighlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return PointHighlighter;

});
