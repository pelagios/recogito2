/**
 * NOTE: there is almost 100% redundancy with the tiltedBoxLayer implementation. If we decide
 * to keep the linkedBoxLayer (i.e. promote it to an offical feature, available to anyone) then
 * this redundancy needs to be (and can easily be) removed.
 */
define([
  'document/annotation/image/selection/layers/geom2D',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/linkedbox/linkedBox'
], function(Geom2D, Layer, LinkedBox) {

  var TWO_PI = 2 * Math.PI;

  var LinkedBoxLayer = function(olMap) {

    var self = this,

        annotations = [],

        computeSize = function(annotation) {
          var anchor = LinkedBox.parseAnchor(annotation.anchor);
          return Geom2D.getPolygonArea(anchor.coords);
        },

        getAnnotationAt = function(e) {
          var coord = e.coordinate, found = [];

          jQuery.each(annotations, function(idx, annotation) {
            var anchor = LinkedBox.parseAnchor(annotation.anchor, 5);
            if(Geom2D.intersects(coord[0], coord[1], anchor.coords))
              found.push({
                annotation: annotation,
                imageBounds: Geom2D.coordsToBounds(anchor.coords)
              });
          });

          // Sort by size, ascending
          found.sort(function(a, b) {
            var sizeA = computeSize(a.annotation),
                sizeB = computeSize(b.annotation);

            return sizeA - sizeB;
          });
          return found[0];
        },

        findById = function(id) {
          var foundAnnotation;
          jQuery.each(annotations, function(idx, annotation) {
            if (annotation.annotation_id === id) {
              foundAnnotation = annotation;
              return false; // Break loop
            }
          });

          if (foundAnnotation)
            return {
              annotation: foundAnnotation,
              imageBounds: Geom2D.coordsToBounds(LinkedBox.parseAnchor(foundAnnotation.anchor).coords)
            };
        },

        addAnnotation = function(annotation, renderImmediately) {
          annotations.push(annotation);
          if (renderImmediately) layer.getSource().changed();
        },

        redraw = function() {
          layer.getSource().changed();
        },

        drawOne = function(annotation, extent, scale, ctx, color) {
          var setStyles = function() {
                ctx.fillStyle = color;
                ctx.strokeStyle = color;
                ctx.lineWidth = 1;
              },

              // Helper function to trace a rectangle path
              traceRect = function() {
                ctx.moveTo(rect[0][0], rect[0][1]);
                ctx.lineTo(rect[1][0], rect[1][1]);
                ctx.lineTo(rect[2][0], rect[2][1]);
                ctx.lineTo(rect[3][0], rect[3][1]);
                ctx.lineTo(rect[0][0], rect[0][1]);
              },

              drawTether = function() {
                var col = self.getColorRGB(),

                    root = [ scale * (anchor.rx - extent[0]), scale * (extent[3] + anchor.ry) ];

                // Filled circle
                ctx.beginPath();
                ctx.globalAlpha = 1.0;
                ctx.arc(root[0], root[1], 2.5, 0, TWO_PI);
                ctx.fill();
                ctx.globalAlpha = self.getStrokeOpacity();
                ctx.arc(root[0], root[1], 5, 0, TWO_PI);
                ctx.stroke();
                ctx.closePath();

                // Line
                ctx.beginPath();
                ctx.moveTo(root[0], root[1]);
                ctx.lineTo(rect[0][0], rect[0][1]);
                ctx.stroke();
                ctx.closePath();
              },

              drawBox = function() {
                var col = self.getColorRGB();
                // Fill
                ctx.globalAlpha = self.getFillOpacity();
                ctx.beginPath();
                traceRect();
                ctx.fill();
                ctx.closePath();

                // Outline
                ctx.globalAlpha = self.getStrokeOpacity();
                ctx.beginPath();
                traceRect();
                ctx.stroke();
                ctx.closePath();
              },

              anchor = LinkedBox.parseAnchor(annotation.anchor),

              rect = anchor.coords.map(function(pt) {
                return [ scale * (pt[0] - extent[0]), scale * (extent[3] - pt[1]) ];
              });

          rect.push(rect[0]); // Close the rect path

          setStyles();
          drawTether();
          drawBox();
        },

        /** Drawing loop that renders all annotations to the drawing area **/
        redrawAll = function(extent, resolution, pixelRatio, size, projection) {
          var canvas = document.createElement('canvas'),
              ctx = canvas.getContext('2d'),
              color = self.getColorRGB();

          canvas.width = size[0];
          canvas.height = size[1];

          jQuery.each(annotations, function(idx, annotation) {
            drawOne(annotation, extent, pixelRatio / resolution, ctx, color);
          });

          return canvas;
        },

        layer = new ol.layer.Image({
          source: new ol.source.ImageCanvas({
            canvasFunction: redrawAll,
            projection: 'ZOOMIFY'
          })
        }),

        removeAnnotation = function(annotation) {
          var idx = annotations.indexOf(annotation);
          if (idx >= 0) {
            annotations.splice(idx, 1);
            redraw();
          }
        },

        convertSelectionToAnnotation = function(selection) {
          addAnnotation(selection.annotation, true);
        },

        emphasiseAnnotation = function(annotation) {
          // TODO for future use
        },

        toggleVisibility = function() {
          layer.setVisible(!layer.getVisible());
        };

    olMap.addLayer(layer);

    this.computeSize = computeSize;
    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.redraw = redraw;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;
    this.toggleVisibility = toggleVisibility;

    Layer.apply(this, [ olMap ]);
  };
  LinkedBoxLayer.prototype = Object.create(Layer.prototype);

  return LinkedBoxLayer;

});
