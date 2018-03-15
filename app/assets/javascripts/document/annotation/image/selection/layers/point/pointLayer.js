define([
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Layer, Style) {

  var MIN_SELECTION_DISTANCE = 10,

      pointToBounds = function(xy) {
        return { top: - xy[1], right: xy[0], bottom: - xy[1], left: xy[0], width: 0, height: 0 };
      };

  var PointLayer = function(olMap) {

    var self = this,

        pointVectorSource = new ol.source.Vector({}),

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

        /** Note that this method breaks for annotations that are not point annotations! **/
        addAnnotation = function(annotation) {
          var anchor = annotation.anchor,
              x = parseInt(anchor.substring(anchor.indexOf(':') + 1, anchor.indexOf(','))),
              y = - parseInt(anchor.substring(anchor.indexOf(',') + 1)),
              pointFeature = new ol.Feature({
                'geometry': new ol.geom.Point([ x, y ])
              });

          pointFeature.set('annotation', annotation, true);
          pointVectorSource.addFeature(pointFeature);
        },

        render = function() {
          // Do nothing - the point layer renders immediately in addAnnotation
        },

        refreshAnnotation = function(annotation) {
          // TODO style change depending on annotation properties
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

    olMap.addLayer(new ol.layer.Vector({
      source: pointVectorSource,
      style: Style.POINT
    }));

    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.render = render;
    this.refreshAnnotation = refreshAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;
    this.computeSize = function() { return 0; }; // It's a point

    Layer.apply(this, [ olMap ]);
  };
  PointLayer.prototype = Object.create(Layer.prototype);

  return PointLayer;

});
