define([
  'common/config',
  'common/hasEvents',
  'document/annotation/image/selection/layers/geom2D'
], function(Config, HasEvents, Geom2D) {

      /** Constants **/
  var TWO_PI = 2 * Math.PI,

      HANDLE_RADIUS = 6,

      // TODO we can move this into a super class later on
      imageToCanvas = function(olMap, xy) {
        return olMap.getPixelFromCoordinate([xy[0], xy[1]])
          .map(function(v) { return Math.round(v); });
      },

      canvasToImage = function(olMap, xy) {
        return olMap.getCoordinateFromPixel([xy[0], xy[1]])
          .map(function(v) { return Math.round(v); });
      };

  var RectDrawingTool = function(canvas, olMap, opt_selection) {

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
              canvasStart : imageToCanvas(olMap, [ b.left, - b.top ]),
              canvasEnd   : imageToCanvas(olMap, [ b.right, - b.bottom ]),
              imageStart  : [ b.left, - b.top ],
              imageEnd    : [ b.right, - b.bottom ]
            };
          } else {
            return false;
          }
        })(),

        /** Updates the shape with the given diff, converting canvas/image coords if needed **/
        updateShape = function(diff) {
          currentShape = (currentShape) ? jQuery.extend({}, currentShape, diff) : diff;

          if (diff.canvasStart && !diff.imageStart)
            currentShape.imageStart = canvasToImage(olMap, diff.canvasStart);

          if (diff.canvasEnd && !diff.imageEnd)
            currentShape.imageEnd = canvasToImage(olMap, diff.canvasEnd);

          if (diff.imageStart && !diff.canvasStart)
            currentShape.canvasStart = imageToCanvas(olMap, diff.imageStart);

          if (diff.imageEnd && !diff.canvasEnd)
            currentShape.canvasEnd = imageToCanvas(olMap, diff.imageEnd);
        },

        /** Returns the bounds of the current shape (if any) in canvas coordinate space **/
        getCanvasBounds = function() {
          if (currentShape)
            return {
              top    : Math.min(currentShape.canvasStart[1], currentShape.canvasEnd[1]),
              right  : Math.max(currentShape.canvasStart[0], currentShape.canvasEnd[0]),
              bottom : Math.max(currentShape.canvasStart[1], currentShape.canvasEnd[1]),
              left   : Math.min(currentShape.canvasStart[0], currentShape.canvasEnd[0]),
              width  : Math.abs(currentShape.canvasEnd[0] - currentShape.canvasStart[0]),
              height : Math.abs(currentShape.canvasEnd[1] - currentShape.canvasStart[1])
            };
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
            currentShape.canvasStart = imageToCanvas(olMap, currentShape.imageStart);
            currentShape.canvasEnd = imageToCanvas(olMap, currentShape.imageEnd);
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
                    return mouseX <= x + HANDLE_RADIUS &&
                           mouseX >= x - HANDLE_RADIUS &&
                           mouseY >= y - HANDLE_RADIUS &&
                           mouseY <= y + HANDLE_RADIUS;
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
          mouseX = e.offsetX;
          mouseY = e.offsetY;

          if (isDrawing) {
            // Stop drawing
            isDrawing = false;
            if (!opt_selection) self.fireEvent('create', getSelection());
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
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          // Don't drag the background map if the shape is being modfied
          if (isModifying) canvas.setForwardEvents(false);
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
            var b = getCanvasBounds(),

                // TODO redundant - can derive this more easily from currentShape.imageBounds

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
              canvasBounds: getCanvasBounds(), // Bounds in canvas coordinate system
              imageBounds: { top: y, right: x + w, bottom: y + h, left: x } // Bounds in original image
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
              };

          // Rect
          ctx.save();
          ctx.translate(centerX, centerY);
          ctx.rotate(rot);
          strokeRect(w, h, 2, strokeColor);
          strokeRect(w + 1, h + 1, 1, '#000');
          ctx.restore();

          // Start/end dots
          drawDot(ctx, currentShape.canvasStart, { hover: hoverTarget === 'START_HANDLE' });
          drawDot(ctx, currentShape.canvasEnd, { hover: hoverTarget === 'END_HANDLE' });
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

    HasEvents.apply(this);
  };
  RectDrawingTool.prototype = Object.create(HasEvents.prototype);

  return RectDrawingTool;

});
