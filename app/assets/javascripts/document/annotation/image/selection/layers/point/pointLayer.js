define([
  'document/annotation/image/selection/layers/layer'
], function(Layer) {

  var MIN_SELECTION_DISTANCE = 10,

      pointToBounds = function(xy) {
        return { top: xy[1], right: xy[0], bottom: xy[1], left: xy[0], width: 0, height: 0 };
      };

  var PointLayer = function(olMap) {
    Layer.apply(this, [ olMap ]);

    var self = this,

        pointVectorSource = new ol.source.Vector({}),

        buildStyle = function() {
          var col = self.getColorRGB(),

              outerCircle = new ol.style.Style({
                image: new ol.style.Circle({
                  radius: 7.4,
                  fill  : new ol.style.Fill({ color: '#fff' }),
                  stroke: new ol.style.Stroke({ color: '#000', width: 1.1 })
                })
              }),

              innerCircle = new ol.style.Style({
                image: new ol.style.Circle({
                  radius: 4.4,
                  fill  : new ol.style.Fill({ color: self.getColorHex() }),
                  stroke: new ol.style.Stroke({ color: 'rgba(0,0,0,0)', width: 1.1 })
                })
              });

          return [ outerCircle, innerCircle ];
        },

        pointVectorLayer = new ol.layer.Vector({
          source: pointVectorSource,
          style: buildStyle()
        }),

        getAnnotationAt = function(e) {
          var closestFeature = pointVectorSource.getClosestFeatureToCoordinate(e.coordinate),
              closestPoint = (closestFeature) ?
                closestFeature.getGeometry().getClosestPoint(e.coordinate) : false;

          if (closestPoint && self.computePxDistance(e.pixel, closestPoint) < MIN_SELECTION_DISTANCE) {
            return {
              annotation: closestFeature.get('annotation'),
              imageBounds: pointToBounds(closestPoint)
            };
          }
        },

        findById = function(id) {
          var feature = self.findFeatureByAnnotationId(id, pointVectorSource),
              point;
          if (feature) {
            point = feature.getGeometry().getCoordinates();
            return {
              annotation: feature.get('annotation'),
              imageBounds: pointToBounds(point)
            };
          }
        },

        addAnnotation = function(annotation, renderImmediately) {
          var anchor = annotation.anchor,
              x = parseInt(anchor.substring(anchor.indexOf(':') + 1, anchor.indexOf(','))),
              y = - parseInt(anchor.substring(anchor.indexOf(',') + 1)),
              pointFeature = new ol.Feature({
                'geometry': new ol.geom.Point([ x, y ])
              });

          pointFeature.set('annotation', annotation, true);
          pointVectorSource.addFeature(pointFeature);
        },

        redraw = function() {
          pointVectorLayer.setStyle(buildStyle());
        },

        removeAnnotation = function(annotation) {
          var feature = self.findFeatureByAnnotationId(annotation.annotation_id, pointVectorSource);
          if (feature)
            pointVectorSource.removeFeature(feature);
        },

        convertSelectionToAnnotation = function(selection) {
          addAnnotation(selection.annotation);
        },

        emphasiseAnnotation = function(annotation) {
          // TODO style change?
        };

    olMap.addLayer(pointVectorLayer);

    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.redraw = redraw;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;
    this.computeSize = function() { return 0; }; // It's a point
  };
  PointLayer.prototype = Object.create(Layer.prototype);

  return PointLayer;

});
