define([
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Layer, Style) {

  var MIN_SELECTION_DISTANCE = 10;

  var PointLayer = function(containerEl, olMap) {

    var self = this,

        pointVectorSource = new ol.source.Vector({}),

        getAnnotationAt = function(e) {
          var closestFeature = pointVectorSource.getClosestFeatureToCoordinate(e.coordinate),
              closestPoint = (closestFeature) ?
                closestFeature.getGeometry().getClosestPoint(e.coordinate) : false;

          if (closestPoint && self.computePxDistance(e.pixel, closestPoint) < MIN_SELECTION_DISTANCE) {
            return {
              annotation: feature.get('annotation'),
              mapBounds: self.pointToBounds(feature.getGeometry().getCoordinates())
            };
          }
        },

        findById = function(id) {

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

        },

        removeAnnotation = function(annotation) {

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

    Layer.apply(this, [ containerEl, olMap ]);
  };
  PointLayer.prototype = Object.create(Layer.prototype);

  return PointLayer;

});
