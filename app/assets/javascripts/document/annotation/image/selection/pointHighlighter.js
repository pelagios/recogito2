define([
  'common/config',
  'document/annotation/common/selection/abstractHighlighter'],

  function(Config, AbstractHighlighter) {

    var POINT_LAYER_STYLE = new ol.style.Style({
          image: new ol.style.Circle({
            radius : 6,
            fill   : new ol.style.Fill({ color: '#4483C4', opacity: 0.35 }),
            stroke : new ol.style.Stroke({ color: '#366696', width: 1.5 })
          })
        });

    var PointHighlighter = function(olMap) {

      var pointVectorSource = new ol.source.Vector({}),

          selectPoint = function(coordinate) {
            var pointFeature = new ol.Feature({
                  'geometry': new ol.geom.Point(coordinate)
                });

            pointVectorSource.addFeature(pointFeature);
          },

          getAnnotationsAt = function() {
            // TODO implement
            console.log('getAnnotationsAt');
          },

          refreshAnnotation = function(annotation) {
            // TODO implement
            console.log('refreshAnnotation', annotation);
          },

          convertSelectionToAnnotation = function(selection) {
            // TODO implement
            console.log('convertSelectionToAnnotation', selection);
          };

      olMap.addLayer(new ol.layer.Vector({
        source: pointVectorSource,
        style: POINT_LAYER_STYLE
      }));

      this.getAnnotationsAt = getAnnotationsAt;
      this.refreshAnnotation = refreshAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;

      AbstractHighlighter.apply(this);
    };
    PointHighlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return PointHighlighter;

});
