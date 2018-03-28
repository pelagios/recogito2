define([
  'document/annotation/common/selection/abstractHighlighter',
  'document/annotation/image/selection/layers/point/pointLayer',
  'document/annotation/image/selection/layers/rect/rectLayer',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBoxLayer'
], function(AbstractHighlighter, PointLayer, RectLayer, TiltedBoxLayer) {

    var Highlighter = function(olMap) {

          /** The list of layer implementations **/
      var layers = {
            tbox  : new TiltedBoxLayer(olMap),
            rect  : new RectLayer(olMap),
            point : new PointLayer(olMap)
          },

          /** Returns the layer appropriate to the annotation **/
          getLayer = function(annotation) {
            var anchor = annotation.anchor,
                shapeType = anchor.substring(0, anchor.indexOf(':')).toLowerCase();

            return layers[shapeType];
          },

          computeSize = function(annotation) {
            var layer = getLayer(annotation);
            return layer.computeSize(annotation);
          },

          getAnnotationAt = function(e) {
            var allAnnotations = [];

            jQuery.each(layers, function(key, layer) {
              var result = layer.getAnnotationAt(e);
              if (result)
               allAnnotations.push(result);
            });

            // Sort by size, ascending
            allAnnotations.sort(function(a, b) {
              return computeSize(a.annotation) - computeSize(b.annotation);
            });

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
              // Add annotations, but don't (necessarily) render them immediatly
              if (layer) layer.addAnnotation(a, false);
            });

            jQuery.each(layers, function(key, layer) {
              layer.redraw();
            });
          },

          /** @override **/
          refreshAnnotation = function(annotation) {
            // TODO for future use (style change based on annotation properties)
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
          },

          setAnnotationColor = function(color) {
            for (var key in layers)
              layers[key].setColor(color);
          };

      this.getAnnotationAt = getAnnotationAt;
      this.findById = findById;
      this.initPage = initPage;
      this.refreshAnnotation = refreshAnnotation;
      this.removeAnnotation = removeAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;
      this.emphasiseAnnotation = emphasiseAnnotation;
      this.setAnnotationColor = setAnnotationColor;

      AbstractHighlighter.apply(this);
    };
    Highlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return Highlighter;

});
