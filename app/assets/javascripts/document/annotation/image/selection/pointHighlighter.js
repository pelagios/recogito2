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

      this.getAnnotationsAt = getAnnotationsAt;
      this.refreshAnnotation = refreshAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;

      AbstractHighlighter.apply(this);
    };
    PointHighlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return PointHighlighter;

});
