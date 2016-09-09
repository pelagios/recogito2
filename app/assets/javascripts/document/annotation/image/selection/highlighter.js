define([
  'document/annotation/common/selection/abstractHighlighter',
  'document/annotation/image/selection/layers/point/pointLayer',
  'document/annotation/image/selection/layers/rect/rectLayer',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBoxLayer'
], function(AbstractHighlighter, PointLayer, RectLayer, TiltedBoxLayer) {

    var Highlighter = function(olMap) {

          /** The list of layer implementations **/
      var layers = {
            point : new PointLayer(olMap),
            rect  : new RectLayer(olMap),
            tbox  : new TiltedBoxLayer(olMap)
          },

          /** Returns the layer appropriate to the annotation **/
          getLayer = function(annotation) {
            var anchor = annotation.anchor,
                shapeType = anchor.substring(0, anchor.indexOf(':')).toLowerCase();

            return layers[shapeType];
          },

          getAnnotationAt = function(e) {
            var allAnnotations = [];

            jQuery.each(layers, function(key, layer) {
              var result = layer.getAnnotationAt(e);
              if (result)
               allAnnotations.push(result);
            });

            // TODO sort by size and pick smallest

            if (allAnnotations.length > 0)
              return allAnnotations[0];
          },

          /** @override **/
          findById = function(id) {
            var found = [];

            jQuery.each(layers, function(key, layer) {
              var result = layer.findById(id);
              if (result)
               found.push(result);
            });

            if (found.length > 0)
              return found[0];
          },

          /** @override **/
          initPage = function(annotations) {
            jQuery.each(annotations, function(idx, a) {
              var layer = getLayer(a);
              if (layer) layer.addAnnotation(a);
            });

            jQuery.each(layers, function(key, layer) {
              layer.render();
            });
          },

          /** @override **/
          refreshAnnotation = function(annotation) {
            var layer = getLayer(annotation);
            if (layer) layer.refreshAnnotation(annotation);
          },

          /** @override **/
          removeAnnotation = function(annotation) {
            var layer = getLayer(annotation);
            if (layer) layer.removeAnnotation(annotation);
          },

          /** @override **/
          convertSelectionToAnnotation = function(selection) {
            var layer = getLayer(selection.annotation);
            if (layer) layer.convertSelectionToAnnotation(selection);
          },

          emphasiseAnnotation = function(annotation) {
            var layer = getLayer(annotation);
            if (layer) layer.emphasiseAnnotation(annotation);
          };

      this.getAnnotationAt = getAnnotationAt;
      this.findById = findById;
      this.initPage = initPage;
      this.refreshAnnotation = refreshAnnotation;
      this.removeAnnotation = removeAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;
      this.emphasiseAnnotation = emphasiseAnnotation;

      AbstractHighlighter.apply(this);
    };
    Highlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return Highlighter;

});
