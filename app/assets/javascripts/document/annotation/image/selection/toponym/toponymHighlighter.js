define(['common/config'], function(Config) {

  /** Drawing styles **/
  var RED = '#aa0000',
TWO_PI = 2 * Math.PI,
  CIRCLE_RADIUS = 3,
  LINE_WIDTH = 2,
  BOX_OPACITY = 0.3;

  /** Helper to compute the rectangle from an annotation geometry **/
  var getRect = function(anchor, opt_minheight) {
        var minheight = (opt_minheight) ? opt_minheight : 0,

            args = anchor.substring(anchor.indexOf(':') + 1).split(','),

            // TODO make this robust against change of argument order
            x = parseInt(args[0].substring(2)),
            y = parseInt(args[1].substring(2)),
            a = parseFloat(args[2].substring(2)),
            l = parseFloat(args[3].substring(2)),
            h = parseFloat(args[4].substring(2)),

            // Make sure annotation is clickable, even with a height of null
            height = (Math.abs(h) > 1) ? h : minheight,

            p1 = { x: x, y: y },
            p2 = { x: x + Math.cos(a) * l, y: y - Math.sin(a) * l },
            p3 = { x: p2.x - height * Math.sin(a), y: p2.y - height * Math.cos(a) },
            p4 = { x: x - height * Math.sin(a), y: y - height * Math.cos(a) };

        return [ p1, p2, p3, p4 ];
      };

  var ToponymHighlighter = function(olMap) {

    var annotations = [],

        renderAnnotation = function(annotation) {
          // Due to the way OL works, annotations are not actually rendered until OL
          // triggers the drawing loop. We just store the annotation in the list.
          annotations.push(annotation);
        },

        drawOne = function(annotation, extent, scale, ctx) {
          var rect = jQuery.map(getRect(annotation.anchor), function(pt) {
            return { x: scale * (pt.x - extent[0]), y: scale * (pt.y + extent[3]) };
          });
          rect.push(rect[0]); // Close path

          // Helper function to trace the rectangle path
          var traceRect = function() {
            ctx.moveTo(rect[0].x, rect[0].y);
            ctx.lineTo(rect[1].x, rect[1].y);
            ctx.lineTo(rect[2].x, rect[2].y);
            ctx.lineTo(rect[3].x, rect[3].y);
            ctx.lineTo(rect[0].x, rect[0].y);
          };

          // Draw rectangle
          ctx.fillStyle = RED;
          ctx.strokeStyle = RED;
          ctx.lineWidth = 1;
          ctx.globalAlpha = 1;

          // if (!annotation.id || annotation.id != currentEdit.id) {
            ctx.beginPath();
            traceRect();
            ctx.stroke();
            ctx.fill();
            ctx.closePath();
          // }

          // Draw rectangle outline
          ctx.globalAlpha = 1;
          ctx.beginPath();
          traceRect();
          ctx.stroke();
          ctx.closePath();
          ctx.globalAlpha = 1;

          // Draw anchor dot
          ctx.beginPath();
          ctx.arc(rect[0].x, rect[0].y, 10, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();

          // Draw baseline
          ctx.lineWidth = 2;
          ctx.strokeStyle = RED;
          ctx.beginPath();
          ctx.moveTo(rect[0].x, rect[0].y);
          ctx.lineTo(rect[1].x, rect[1].y);
          ctx.stroke();
          ctx.closePath();
        },

        /** Drawing loop that renders all annotations to the drawing area **/
        redrawAll = function(extent, resolution, pixelRatio, size, projection) {
          var canvas = document.createElement('canvas'),
              ctx = canvas.getContext('2d');

          canvas.width = size[0];
          canvas.height = size[1];

          // TODO optimize so that stuff outside the visible area isn't drawn
          jQuery.each(annotations, function(idx, annotation) {
            drawOne(annotation, extent, pixelRatio / resolution, ctx);
          });

          return canvas;
        },

        layer = new ol.layer.Image({
          source: new ol.source.ImageCanvas({
            canvasFunction: redrawAll,
            projection: 'ZOOMIFY'
          })
        }),

        refreshAnnotation = function(annotation) {
          // TODO implement
        },

        getCurrentHighlight = function() {
          // TODO implement
        };

    olMap.addLayer(layer);

    this.getCurrentHighlight = getCurrentHighlight;
    this.renderAnnotation = renderAnnotation;
    this.refreshAnnotation = refreshAnnotation;
  };

  return ToponymHighlighter;

});
