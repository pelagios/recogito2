define([
  'document/annotation/image/selection/layers/baseDrawingTool',
  'document/annotation/image/selection/layers/geom2D'
], function(BaseTool, Geom2D) {

  var RectDrawingTool = function(canvas, olMap, opt_selection) {
    // Extend at start, so that we have base prototype methods available on init (currentShape!)
    BaseTool.apply(this, [ olMap ]);

    var self = this,

        mouseX, mouseY,

        isDrawing = false,

        isModifying = false,

        // Setting this to false will stop the animation loop
        running = true,

        /**
         * Either false or an object with start/end coordinates in canvas and image systems.
         *
         * { canvasStart: [], canvasEnd: [], imageStart: [], imageEnd: [] }
         */
        currentShape = (function() {
          if (opt_selection) {
            var b = opt_selection.imageBounds;
            return {
              canvasStart : self.imageToCanvas([ b.left, b.top ]),
              canvasEnd   : self.imageToCanvas([ b.right, b.bottom ]),
              imageStart  : [ b.left, b.top ],
              imageEnd    : [ b.right, b.bottom ]
            };
          } else {
            return false;
          }
        })(),

        /** Updates the shape with the given diff, converting canvas/image coords if needed **/
        updateShape = function(diff) {
          // Merge the diff with the current shape
          currentShape = (currentShape) ? jQuery.extend({}, currentShape, diff) : diff;

          // Cross-fill, i.e. update image- from canvas-coords and vice versa
          self.crossFill(diff, currentShape, [
            [ 'canvasStart', 'imageStart' ],
            [ 'canvasEnd', 'imageEnd' ]
          ]);
        },

        /** Returns the bounds of the current shape (if any) in canvas coordinate space **/
        getCanvasBounds = function() {
          if (currentShape) {
            return {
              top    : Math.min(currentShape.canvasStart[1], currentShape.canvasEnd[1]),
              right  : Math.max(currentShape.canvasStart[0], currentShape.canvasEnd[0]),
              bottom : Math.max(currentShape.canvasStart[1], currentShape.canvasEnd[1]),
              left   : Math.min(currentShape.canvasStart[0], currentShape.canvasEnd[0]),
              width  : Math.abs(currentShape.canvasEnd[0] - currentShape.canvasStart[0]),
              height : Math.abs(currentShape.canvasEnd[1] - currentShape.canvasStart[1])
            };
          }
        },

        /** Returns the bounds of the current shape (if any) in image coordinate space **/
        getImageBounds = function() {
          if (currentShape)
            return {
              top    : Math.min(currentShape.imageStart[1], currentShape.imageEnd[1]),
              right  : Math.max(currentShape.imageStart[0], currentShape.imageEnd[0]),
              bottom : Math.max(currentShape.imageStart[1], currentShape.imageEnd[1]),
              left   : Math.min(currentShape.imageStart[0], currentShape.imageEnd[0]),
              width  : Math.abs(currentShape.imageEnd[0] - currentShape.imageStart[0]),
              height : Math.abs(currentShape.imageEnd[1] - currentShape.imageStart[1])
            };
        },

        /** Refreshes the canvas position of the current shape, according to the image state **/
        refreshPosition = function() {
          if (currentShape) {
            currentShape.canvasStart = self.imageToCanvas(currentShape.imageStart);
            currentShape.canvasEnd = self.imageToCanvas(currentShape.imageEnd);
          }
        },

        shiftShape = function(dx, dy) {
          if (currentShape)
            updateShape({
              canvasStart: [ currentShape.canvasStart[0] + dx, currentShape.canvasStart[1] + dy ],
              canvasEnd:   [ currentShape.canvasEnd[0] + dx, currentShape.canvasEnd[1] + dy ]
            });
        },

        shiftHandle = function(handle, dx, dy) {
          if (handle === 'START_HANDLE')
            updateShape({ canvasStart: [
              currentShape.canvasStart[0] + dx, currentShape.canvasStart[1] + dy
            ]});
          else if (handle == 'END_HANDLE')
            updateShape({ canvasEnd: [
              currentShape.canvasEnd[0] + dx, currentShape.canvasEnd[1] + dy
            ]});
        },

        getHoverTarget = function() {
          var isOverHandle = function(xy) {
                return function() {
                  if (xy) {
                    var x = xy[0], y = xy[1];
                    return mouseX <= x + BaseTool.HANDLE_RADIUS &&
                           mouseX >= x - BaseTool.HANDLE_RADIUS &&
                           mouseY >= y - BaseTool.HANDLE_RADIUS &&
                           mouseY <= y + BaseTool.HANDLE_RADIUS;
                  }
                };
              },

              isOverStartHandle = isOverHandle(currentShape.canvasStart),
              isOverEndHandle = isOverHandle(currentShape.canvasEnd),

              isOverShape = function() {
                var bounds = getImageBounds(),
                    hoverXY = olMap.getCoordinateFromPixel([mouseX, mouseY]);

                if (bounds)
                  return hoverXY[0] >= bounds.left &&
                         hoverXY[0] <= bounds.right &&
                         hoverXY[1] >= bounds.top &&
                         hoverXY[1] <= bounds.bottom;
              };

          if (isOverStartHandle())    return 'START_HANDLE';
          else if (isOverEndHandle()) return 'END_HANDLE';
          else if (isOverShape())     return 'SHAPE';
        },

        onMouseMove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          if (isDrawing)
            updateShape({ canvasEnd: [ mouseX, mouseY ] });
        },

        /** Triggers on click as well as on drag start **/
        onMouseDown = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          isModifying = getHoverTarget();
        },

        onMouseClick = function(e) {
          if (isDrawing) {
            // Stop drawing
            isDrawing = false;
            self.fireEvent('create', getSelection());
          } else if (!currentShape) {
            // Start drawing - unless we already have a shape
            isDrawing = true;
            updateShape({
              canvasStart: [ mouseX, mouseY ],
              canvasEnd: [ mouseX, mouseY ]
            });
          }
        },

        onMouseDrag = function(e) {
          var hoverTarget = getHoverTarget(),
              dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          // Don't drag the background map if the shape is being modfied
          if (isModifying || hoverTarget) canvas.setForwardEvents(false);
          else canvas.setForwardEvents(true);

          if (isModifying === 'START_HANDLE' || isModifying === 'END_HANDLE')
            // Move only the handle
            shiftHandle(isModifying, dx, dy);
          else if (isModifying === 'SHAPE')
            // Move the shape
            shiftShape(dx, dy);

          // If it's a modification, fire changeShape event
          if (isModifying) self.fireEvent('changeShape', getSelection());
        },

        getSelection = function() {
          if (currentShape) {
            var imageBounds = getImageBounds(),

                x =   Math.round(imageBounds.left),
                y = - Math.round(imageBounds.bottom),
                w =   Math.round(imageBounds.width),
                h =   Math.round(imageBounds.height),

                anchor = 'rect:x=' + x + ',y=' + y + ',w=' + w + ',h=' + h;

            return self.buildSelection(anchor, getCanvasBounds(), imageBounds);
          }
        },

        drawCurrentShape = function(ctx, hoverTarget) {
          var startX = currentShape.canvasStart[0],
              startY = currentShape.canvasStart[1],

              endX = currentShape.canvasEnd[0],
              endY = currentShape.canvasEnd[1],

              // Center of the rect = center of rotation
              centerX = (startX + endX) / 2,
              centerY = (startY + endY) / 2,

              // One of the corner points, so we can compute the proper dimensions in canvas space
              corner = olMap.getPixelFromCoordinate([
                currentShape.imageEnd[0], currentShape.imageStart[1]
              ]),

              w = Geom2D.len(startX, startY, corner[0], corner[1]),
              h = Geom2D.len(endX, endY, corner[0], corner[1]),

              rot = olMap.getView().getRotation(),

              strokeColor = (hoverTarget === 'SHAPE') ? 'orange' : '#fff';

              // Shorthand
              strokeRect = function(w, h, width, col) {
                ctx.beginPath();
                ctx.shadowBlur = 0;
                ctx.lineWidth = width;
                ctx.strokeStyle = col;
                ctx.rect(-w / 2, - h / 2, w, h);
                ctx.stroke();
                ctx.closePath();
              };

          // Rect
          ctx.save();
          ctx.translate(centerX, centerY);
          ctx.rotate(rot);
          strokeRect(w, h, 2, strokeColor);
          strokeRect(w + 1, h + 1, 1, '#000');
          ctx.restore();

          // Start/end dots
          self.drawHandle(ctx, currentShape.canvasStart, { hover: hoverTarget === 'START_HANDLE' });
          self.drawHandle(ctx, currentShape.canvasEnd, { hover: hoverTarget === 'END_HANDLE' });
        },

        render = function() {
          var hoverTarget = getHoverTarget();

          canvas.clear();
          if (currentShape) drawCurrentShape(canvas.ctx, hoverTarget);

          if (!hoverTarget)
            canvas.setCursor('crosshair'); // Not hovering over shape? Default crosshair cursor
          else if (hoverTarget === 'SHAPE')
            canvas.setCursor('move'); // Hovering over the shape? MOVE/GRAB cursor
          else
            canvas.setCursor(); // Handles? No cursor

          if (running) requestAnimationFrame(render);
        },

        reset = function() {
          currentShape = false;
          isDrawing = false;
          isModifying = false;
        },

        destroy = function() {
          running = false;
          canvas.off('mousemove');
          canvas.off('mousedown');
          canvas.off('click');
          canvas.off('drag');
        };

    // Set default crosshair cursor
    canvas.setCursor('crosshair');

    // Attach mouse handlers
    canvas.on('mousemove', onMouseMove);
    canvas.on('mousedown', onMouseDown);
    canvas.on('click', onMouseClick);
    canvas.on('drag', onMouseDrag);

    this.refreshPosition = refreshPosition;
    this.getSelection = getSelection;
    this.reset = reset;
    this.destroy = destroy;

    // Start rendering loop
    render();
  };
  RectDrawingTool.prototype = Object.create(BaseTool.prototype);

  return RectDrawingTool;

});
