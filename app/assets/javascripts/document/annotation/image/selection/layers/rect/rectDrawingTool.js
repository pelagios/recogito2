define([
  'common/config',
  'common/hasEvents'
], function(Config, HasEvents) {

      /** Constants **/
  var TWO_PI = 2 * Math.PI,

      HANDLE_RADIUS = 6;

  var RectDrawingTool = function(canvas, olMap, opt_selection) {

    var self = this,

        mouseX, mouseY,

        isDrawing = false,

        isModifying = false,

        // Setting this to false will stop the animation loop
        running = true,

        // { start: [], end [] } or false
        currentShape = (function() {
          if (opt_selection)
            return {
              start: olMap.getPixelFromCoordinate([
                  opt_selection.origBounds.left,
                - opt_selection.origBounds.top
              ]).map(function(v) { return Math.round(v); }),

              end: olMap.getPixelFromCoordinate([
                  opt_selection.origBounds.right,
                - opt_selection.origBounds.bottom
              ]).map(function(v) { return Math.round(v); })
            };
          else
            return false;
        })(),

        getShapeBounds = function() {
          if (currentShape)
            return {
              top    : Math.min(currentShape.start[1], currentShape.end[1]),
              right  : Math.max(currentShape.start[0], currentShape.end[0]),
              bottom : Math.max(currentShape.start[1], currentShape.end[1]),
              left   : Math.min(currentShape.start[0], currentShape.end[0]),
              width  : Math.abs(currentShape.end[0] - currentShape.start[0]),
              height : Math.abs(currentShape.end[1] - currentShape.start[1])
            };
        },

        shiftSelection = function(dx, dy) {
          if (currentShape) {
            currentShape.start =
              [ currentShape.start[0] + dx, currentShape.start[1] + dy ];
            currentShape.end =
              [ currentShape.end[0] + dx, currentShape.end[1] + dy ];
          }
        },

        shiftHandle = function(handle, dx, dy) {
          if (handle === 'START_HANDLE')
            currentShape.start =
              [ currentShape.start[0] + dx, currentShape.start[1] + dy ];
          else if (handle == 'END_HANDLE')
            currentShape.end =
              [ currentShape.end[0] + dx, currentShape.end[1] + dy ];
        },

        getHoverTarget = function() {
          var isOverHandle = function(xy) {
                return function() {
                  if (xy) {
                    var x = xy[0], y = xy[1];
                    return mouseX <= x + HANDLE_RADIUS &&
                           mouseX >= x - HANDLE_RADIUS &&
                           mouseY >= y - HANDLE_RADIUS &&
                           mouseY <= y + HANDLE_RADIUS;
                  }
                };
              },

              isOverStartHandle = isOverHandle(currentShape.start),
              isOverEndHandle = isOverHandle(currentShape.end),

              isOverShape = function() {
                var bounds = getShapeBounds();
                if (bounds)
                  return mouseX >= bounds.left &&
                         mouseX <= bounds.right &&
                         mouseY >= bounds.top &&
                         mouseY <= bounds.bottom;
              };

          if (isOverStartHandle())
            return 'START_HANDLE';
          else if (isOverEndHandle())
            return 'END_HANDLE';
          else if (isOverShape())
            return 'SHAPE';
        },

        onMouseMove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          if (isDrawing) currentShape.end = [ mouseX, mouseY ];
        },

        /** Triggers on click as well as on drag start **/
        onMouseDown = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          isModifying = getHoverTarget();
        },

        onMouseDrag = function(e) {
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          // Don't drag the background map if the shape is being modfied
          if (isModifying) canvas.setForwardEvents(false);
          else canvas.setForwardEvents(true);

          if (isModifying === 'START_HANDLE' || isModifying === 'END_HANDLE')
            // Move only the handle
            shiftHandle(isModifying, dx, dy);
          else
            // Move the shape on the screen (with or without background map)
            shiftSelection(dx, dy);

          // If it's a modification, fire changeShape event
          if (isModifying) self.fireEvent('changeShape', getSelection());
        },

        onMouseClick = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;

          if (isDrawing) {
            // Stop drawing
            isDrawing = false;
            if (!opt_selection) self.fireEvent('create', getSelection());
          } else {
            // Start drawing
            isDrawing = true;
            currentShape = {
              start: [ mouseX, mouseY ],
              end:   [ mouseX, mouseY ]
            };
          }
        },

        getSelection = function() {
          if (currentShape) {
            var b = getShapeBounds(),

                topLeft = olMap.getCoordinateFromPixel([ b.left, b.top ]),
                bottomRight = olMap.getCoordinateFromPixel([ b.right, b.bottom ]),

                x =   Math.round(topLeft[0]),
                y = - Math.round(topLeft[1]),
                w =   Math.round(bottomRight[0] - topLeft[0]),
                h =   Math.round(topLeft[1] - bottomRight[1]),

                anchor = 'rect:x=' + x + ',y=' + y + ',w=' + w + ',h=' + h;

                annotation = {
                  annotates: {
                    document_id: Config.documentId,
                    filepart_id: Config.partId,
                    content_type: Config.contentType
                  },
                  anchor: anchor,
                  bodies: []
                };

            return {
              annotation: annotation,
              canvasBounds: getShapeBounds(), // Bounds in canvas coordinate system
              origBounds: { top: y, right: x + w, bottom: y + h, left: x } // Bounds in original image
            };
          }
        },

        drawDot = function(ctx, xy, opts) {
          var hasBlur = (opts) ? opts.blur || opts.hover : false, // Hover implies blur
              isHover = (opts) ? opts.hover : false;

          // Black Outline
          ctx.beginPath();
          ctx.lineWidth = 4;
          ctx.shadowBlur = (hasBlur) ? 6 : 0;
          ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';
          ctx.strokeStyle = 'rgba(0, 0, 0, 0.8)';
          ctx.arc(xy[0], xy[1], HANDLE_RADIUS, 0, TWO_PI);
          ctx.stroke();

          // Inner dot (white stroke + color fill)
          ctx.beginPath();
          ctx.shadowBlur = 0;
          ctx.lineWidth = 2;
          ctx.strokeStyle = '#fff';
          ctx.fillStyle = (isHover) ? 'orange' : '#000';
          ctx.arc(xy[0], xy[1], HANDLE_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.stroke();
        },

        drawCurrentShape = function(ctx, hoverTarget) {
          var bounds = getShapeBounds(),

              strokeColor = (hoverTarget === 'SHAPE') ? 'orange' : '#fff';

              // Shorthand
              strokeRect = function(x, y, w, h, width, col) {
                ctx.beginPath();
                ctx.shadowBlur = 0;
                ctx.lineWidth = width;
                ctx.strokeStyle = col;
                ctx.rect(x, y, w, h);
                ctx.stroke();
              };

          // Rect
          strokeRect(bounds.left, bounds.top, bounds.width, bounds.height, 2, strokeColor);

          // Outline
          strokeRect(bounds.left - 0.5, bounds.top - 0.5, bounds.width + 1, bounds.height + 1, 1, '#000');

          // Corner dots
          drawDot(ctx, currentShape.start, { hover: hoverTarget === 'START_HANDLE' });
          drawDot(ctx, currentShape.end, { hover: hoverTarget === 'END_HANDLE' });
        },

        render = function() {
          var hoverTarget = getHoverTarget();

          canvas.clear();
          if (currentShape) drawCurrentShape(canvas.ctx, hoverTarget);

          // Don't draw cursor if we are drawing or there's a hover target
          if (!(isDrawing || hoverTarget)) drawDot(canvas.ctx, [ mouseX, mouseY ], { blur: true });

          // If we're hovering over the shape, set cursor to hand
          if (hoverTarget === 'SHAPE') canvas.setCursor('move');
          else canvas.setCursor();

          // TODO change, so we can stop the animation on .setEnabled(false)
          if (running) requestAnimationFrame(render);
        },

        clearSelection = function() {
          currentShape = false;
        },

        destroy = function() {
          running = false;
          canvas.off('mousemove');
          canvas.off('mousedown');
          canvas.off('click');
          canvas.off('drag');
        };

    // Attach mouse handlers
    canvas.on('mousemove', onMouseMove);
    canvas.on('mousedown', onMouseDown);
    canvas.on('click', onMouseClick);
    canvas.on('drag', onMouseDrag);

    this.clearSelection = clearSelection;
    this.getSelection = getSelection;
    this.destroy = destroy;

    // Start rendering loop
    render();

    HasEvents.apply(this);
  };
  RectDrawingTool.prototype = Object.create(HasEvents.prototype);

  return RectDrawingTool;

});
