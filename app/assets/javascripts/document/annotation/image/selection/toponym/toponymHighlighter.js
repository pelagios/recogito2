define([
  'common/config',
  'document/annotation/image/selection/toponym/geom2D',
  'document/annotation/image/selection/style'], function(Config, Geom2D, Style) {

      /** Shorthand **/
  var TWO_PI = 2 * Math.PI,

      /** Helper to compute the rectangle from an annotation geometry **/
      anchorToRect = function(anchor, opt_minheight) {
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

        currentHighlight = false,

        addAnnotation = function(annotation, renderImmediately) {
          annotations.push(annotation);
          if (renderImmediately)
            layer.getSource().changed();
        },

        render = function() {
          layer.getSource().changed();
        },

        drawOne = function(annotation, extent, scale, ctx, color) {
              // Helper function to trace a rectangle path
          var traceRect = function() {
                ctx.moveTo(rect[0].x, rect[0].y);
                ctx.lineTo(rect[1].x, rect[1].y);
                ctx.lineTo(rect[2].x, rect[2].y);
                ctx.lineTo(rect[3].x, rect[3].y);
                ctx.lineTo(rect[0].x, rect[0].y);
              },

              setStyles = function() {
                ctx.fillStyle = color;
                ctx.strokeStyle = color;
                ctx.lineWidth = 1;
              },

              drawBox = function() {
                // Fill
                ctx.globalAlpha = Style.BOX_OPACITY;
                ctx.beginPath();
                traceRect();
                ctx.fill();
                ctx.closePath();

                // Outline
                ctx.globalAlpha = 1;
                ctx.beginPath();
                traceRect();
                ctx.stroke();
                ctx.closePath();
              },

              drawBaseLine = function() {
                ctx.lineWidth = Style.BOX_BASELINE_WIDTH;
                ctx.beginPath();
                ctx.moveTo(rect[0].x, rect[0].y);
                ctx.lineTo(rect[1].x, rect[1].y);
                ctx.stroke();
                ctx.closePath();
              },

              drawAnchorDot = function() {
                ctx.beginPath();
                ctx.arc(rect[0].x, rect[0].y, Style.BOX_ANCHORDOT_RADIUS, 0, TWO_PI);
                ctx.fill();
                ctx.closePath();
              },

              rect = jQuery.map(anchorToRect(annotation.anchor), function(pt) {
                return { x: scale * (pt.x - extent[0]), y: scale * (pt.y + extent[3]) };
              });

          rect.push(rect[0]); // Close the rect path

          setStyles();
          drawBox();
          drawBaseLine();
          drawAnchorDot();
        },

        /** Drawing loop that renders all annotations to the drawing area **/
        redrawAll = function(extent, resolution, pixelRatio, size, projection) {
          var canvas = document.createElement('canvas'),
              ctx = canvas.getContext('2d');

          canvas.width = size[0];
          canvas.height = size[1];

          // TODO optimize so that stuff outside the visible area isn't drawn
          jQuery.each(annotations, function(idx, annotation) {
            drawOne(annotation, extent, pixelRatio / resolution, ctx, Style.COLOR_RED);
          });

          return canvas;
        },

        layer = new ol.layer.Image({
          source: new ol.source.ImageCanvas({
            canvasFunction: redrawAll,
            projection: 'ZOOMIFY'
          })
        }),

        getAnnotationsAt = function(coord) {
          // TODO optimize with a spatial tree
          var found = [];
          jQuery.each(annotations, function(idx, annotation) {
            var rect = anchorToRect(annotation.anchor, 5);
            if(Geom2D.intersects(coord[0], - coord[1], rect))
              found.push(annotation);
          });
          return found;
        },

        highlightAnnotation = function(annotation) {
          currentHighlight = annotation;
          if (annotation)
            document.body.style.cursor = 'pointer';
          else
            document.body.style.cursor = 'auto';
        },

        onMouseMove = function(e) {
          // TODO select the smallest hovered annotation, not hovered[0]
          var all = getAnnotationsAt(e.coordinate),
              hovered = (all.length > 0) ? all[0] : false;

          if (hovered) {
            if (currentHighlight) {
              // Highlight changed to another annotation
              if  (currentHighlight.annotation_id !== hovered.annotation_id)
                highlightAnnotation(hovered);
            } else {
              // No previous highlight - just highlight the hovered box
              highlightAnnotation(hovered);
            }
          } else if (currentHighlight) {
            highlightAnnotation(false);
          }
        },

        refreshAnnotation = function(annotation) {
          // TODO implement
        },

        getCurrentHighlight = function() {
          return currentHighlight;
        };

    olMap.addLayer(layer);

    olMap.on('pointermove', onMouseMove);

    this.addAnnotation = addAnnotation;
    this.render = render;
    this.getCurrentHighlight = getCurrentHighlight;
    this.refreshAnnotation = refreshAnnotation;
  };

  return ToponymHighlighter;

});
