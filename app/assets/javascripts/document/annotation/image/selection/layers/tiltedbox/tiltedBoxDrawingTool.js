define([
  'document/annotation/image/selection/layers/baseDrawingTool',
  'document/annotation/image/selection/layers/geom2D'
], function(BaseTool, Geom2D) {

      /** Constants **/
  var TWO_PI = 2 * Math.PI,

      SMALL_HANDLE_RADIUS = 5;

  var TiltedBoxDrawingTool = function(canvas, olMap, opt_selection) {
    // Extend at start, so that we have base prototype methods available on init (currentShape!)
    BaseTool.apply(this, [ olMap ]);

    var self = this,

        mouseX, mouseY,

        /**
         * Either false or an object of the following form:
         *
         * {
         *   anchor: { canvasXY: [], imageXY: [] },
         *   baseEnd: { canvasXY: [], imageXY: [] },
         *   opposite: { canvasXY: [], imageXY: [] }
         * }
         *
         */
        currentShape = false,

        // Painting state flags
        isStateBaseline = false,
        isStateExtrude = false,

        isModifying = false,

        // Setting this to false will stop the animation loop
        running = true,

        updateShape = function(diff) {
          // Perform deep clone
          currentShape = (currentShape) ? jQuery.extend(true, {}, currentShape, diff) : diff;

          if (diff.anchor) self.crossFill(diff.anchor, currentShape.anchor, 'canvasXY', 'imageXY');
          if (diff.baseEnd) self.crossFill(diff.baseEnd, currentShape.baseEnd, 'canvasXY', 'imageXY');
          if (diff.opposite) self.crossFill(diff.opposite, currentShape.opposite, 'canvasXY', 'imageXY');
        },

        shiftShape = function(dx, dy) {
          var shiftCoord = function(coord) {
                return { canvasXY: [ coord.canvasXY[0] + dx, coord.canvasXY[1] + dy ] };
              };

          updateShape({
            anchor   : shiftCoord(currentShape.anchor),
            baseEnd  : shiftCoord(currentShape.baseEnd),
            opposite : shiftCoord(currentShape.opposite)
          });
        },

        /** Computes opposite corner corresponding to the current mouse position **/
        getFloatingOpposite = function() {
          var baseEndX = currentShape.baseEnd.canvasXY[0],
              baseEndY = currentShape.baseEnd.canvasXY[1],

              baseline = getCanvasBaseline(),

              normal = Geom2D.normalize([ - baseline[1], baseline[0] ]),

              // Vector baseline -> mouse
              toMouse = [ mouseX - baseEndX, mouseY - baseEndY ],

              // Projection of toMouse onto normal
              f = [
                normal[0] * Geom2D.len(toMouse) *
                  Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse))),
                normal[1] * Geom2D.len(toMouse) *
                  Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse)))
              ];

          return [ baseEndX + f[0], baseEndY + f[1] ];
        },

        /** Shorthand to get the baseline vector from the current shape **/
        getCanvasBaseline = function() {
          var anchorX = currentShape.anchor.canvasXY[0],
              anchorY = currentShape.anchor.canvasXY[1],

              baseEndX = currentShape.baseEnd.canvasXY[0],
              baseEndY = currentShape.baseEnd.canvasXY[1];

          return [ baseEndX - anchorX, baseEndY - anchorY ];
        },

        /** Shorthand to get the box coordinates of the current shape in canvas coord space **/
        getShapeCanvasCoords = function() {
          var anchor   = currentShape.anchor.canvasXY,
              baseEnd  = currentShape.baseEnd.canvasXY,
              opposite = currentShape.opposite.canvasXY,

              // Vector baseEnd -> opposite
              fx = opposite[0] - baseEnd[0],
              fy = opposite[1] - baseEnd[1];

          // Coordinates in clockwise direction
          return [ anchor, [ anchor[0] + fx, anchor[1] + fy ], opposite, baseEnd ];
        },

        /** Shorthand to get the box coordinates of the current shape in image coord space **/
        getImageCanvasCoords = function() {
          var anchor   = currentShape.anchor.imageXY,
              baseEnd  = currentShape.baseEnd.imageXY,
              opposite = currentShape.opposite.imageXY,

              // Vector baseEnd -> opposite
              fx = opposite[0] - baseEnd[0],
              fy = opposite[1] - baseEnd[1];

          // Coordinates in clockwise direction
          return [ anchor, [ anchor[0] + fx, anchor[1] + fy ], opposite, baseEnd ];
        },

        getHoverTarget = function() {
          var isOverHandle = function(coord, radius) {
                if (coord) {
                  var x = coord.canvasXY[0], y = coord.canvasXY[1];
                  return mouseX <= x + radius && mouseX >= x - radius &&
                         mouseY >= y - radius && mouseY <= y + radius;
                }
              };

          if (currentShape && currentShape.opposite)
            if (Geom2D.intersects(mouseX, mouseY, getShapeCanvasCoords()))
              return 'SHAPE';
            else if (isOverHandle(currentShape.anchor, BaseTool.HANDLE_RADIUS))
              return 'SHAPE';
            else if (isOverHandle(currentShape.baseEnd, SMALL_HANDLE_RADIUS))
              return 'BASE_END_HANDLE';
            else if (isOverHandle(currentShape.opposite, SMALL_HANDLE_RADIUS))
              return 'OPPOSITE_HANDLE';
        },

        onMouseMove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;

          if (isStateBaseline)
            updateShape({ baseEnd: { canvasXY: [ mouseX, mouseY ] } });
        },

        onMouseDown = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          isModifying = getHoverTarget();
        },

        onMouseClick = function(e) {
          if (currentShape) {
            if (isStateBaseline) {
              // Fix the baseline
              isStateBaseline = false;
              isStateExtrude = true;
              updateShape({ baseEnd: { canvasXY: [ mouseX, mouseY ] } });
            } else if (isStateExtrude) {
              // Fix height
              isStateBaseline = false;
              isStateExtrude = false;
              updateShape({ opposite: { canvasXY: getFloatingOpposite() } });
              self.fireEvent('create', getSelection());
            }
          } else {
            // Start new shape
            isStateBaseline = true;
            updateShape({
              anchor: { canvasXY: [ mouseX, mouseY ] },
              baseEnd: { canvasXY: [ mouseX, mouseY ]}
            });
          }
        },

        onMouseDrag = function(e) {
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          // Don't drag the background map if the shape is being modfied
          if (isModifying) canvas.setForwardEvents(false);
          else canvas.setForwardEvents(true);

          if (isModifying === 'SHAPE')
            shiftShape(dx, dy);

          // If it's a modification, fire changeShape event
          if (isModifying) self.fireEvent('changeShape', getSelection());
        },

        drawCurrentShape = function(ctx, hoverTarget) {
          var anchorX = currentShape.anchor.canvasXY[0],
              anchorY = currentShape.anchor.canvasXY[1],

              baseEndX = currentShape.baseEnd.canvasXY[0],
              baseEndY = currentShape.baseEnd.canvasXY[1],

              baseline = [ baseEndX - anchorX, baseEndY - anchorY ],

              // Computes opposite corner coords, based on drawing state
              getOpposite = function() {
                if (isStateExtrude)
                  // Drawing - use mouse position
                  return getFloatingOpposite();
                else
                  // Drawing done - use stored opposite
                  return currentShape.opposite.canvasXY;
              },

              drawSmallHandle = function(x, y, hover) {
                ctx.beginPath();
                ctx.lineWidth = 0.8;
                ctx.shadowBlur = (hover) ? 6 : 0;
                ctx.strokeStyle = '#000';
                ctx.fillStyle = (hover) ? BaseTool.HOVER_COLOR : '#fff';
                ctx.arc(x, y, SMALL_HANDLE_RADIUS, 0, TWO_PI);
                ctx.fill();
                ctx.stroke();
                ctx.shadowBlur = 0;
                ctx.closePath();
              },

              drawBaseline = function() {
                var line = function(fromX, fromY, toX, toY) {
                      ctx.beginPath();
                      ctx.moveTo(fromX, fromY);
                      ctx.lineTo(toX, toY);
                      ctx.stroke();
                      ctx.closePath();
                    };

                ctx.lineWidth = 4.4;
                ctx.strokeStyle = '#000';
                line(anchorX, anchorY, baseEndX, baseEndY);

                ctx.lineWidth = 2.8;
                ctx.strokeStyle = (hoverTarget === 'SHAPE') ? BaseTool.HOVER_COLOR : '#fff';
                line(anchorX, anchorY, baseEndX, baseEndY);
              },

              drawBox = function() {
                var hover = isStateExtrude || hoverTarget === 'OPPOSITE_HANDLE',

                    opposite = getOpposite(),

                    // Vector baseEnd -> opposite
                    fx = opposite[0] - baseEndX,
                    fy = opposite[1] - baseEndY;

                ctx.lineWidth = 0.9;
                ctx.strokeStyle = '#000';

                ctx.beginPath();
                ctx.moveTo(anchorX, anchorY);
                ctx.lineTo(anchorX + fx, anchorY + fy);
                ctx.lineTo(opposite[0], opposite[1]);
                ctx.lineTo(baseEndX, baseEndY);
                ctx.stroke();
                ctx.closePath();

                drawSmallHandle(opposite[0], opposite[1], hover);
              };

          if (!isStateBaseline) drawBox();
          drawBaseline();

          // Baseline end handle
          drawSmallHandle(baseEndX, baseEndY, isStateBaseline || hoverTarget === 'BASE_END_HANDLE');

          // Anchor handle
          self.drawHandle(ctx, [ anchorX, anchorY ], { hover: hoverTarget === 'SHAPE' });
        },

        getSelection = function() {
          if (currentShape) {
            var canvasBounds = self.coordsToBounds(getShapeCanvasCoords()),
                imageBounds = self.coordsToBounds(getImageCanvasCoords()),

                anchorX = currentShape.anchor.imageXY[0],
                anchorY = currentShape.anchor.imageXY[1],

                baseEndX = currentShape.baseEnd.imageXY[0],
                baseEndY = currentShape.baseEnd.imageXY[1],

                oppositeX = currentShape.opposite.imageXY[0],
                oppositeY = currentShape.opposite.imageXY[1],

                // Baseline vector
                dx = baseEndX - anchorX,
                dy = baseEndY - anchorY,

                // Vectore base end -> opposite
                dh = [ oppositeX - baseEndX, oppositeY - baseEndY ],

                corr = (dx < 0 && dy >= 0) ? Math.PI : ((dx < 0 && dy <0) ? - Math.PI : 0),

                baselineAngle = Math.atan(dy / dx) + corr,
                baselineLength = Math.sqrt(dx * dx + dy * dy),
                height = Math.sqrt(dh[0] * dh[0] + dh[1] * dh[1]),

                anchor;

            if (corr === 0 && dh[1] < 0)
              height = -1 * height;
            else if (corr < 0 && dh[0] < 0)
              height = -1 * height;
            else if (corr > 0 && dh[0] > 0)
              height = -1 * height;

            anchor = 'tbox:' +
              'x=' + Math.round(anchorX) + ',' +
              'y=' + Math.round(Math.abs(anchorX)) + ',' +
              'a=' + baselineAngle + ',' +
              'l=' + Math.round(baselineLength) + ',' +
              'h=' + Math.round(height);

            return self.buildSelection(anchor, canvasBounds, imageBounds);
          }
        },

        render = function() {
          var hoverTarget = getHoverTarget();

          canvas.clear();
          if (currentShape) drawCurrentShape(canvas.ctx, hoverTarget);

          if (isStateBaseline || isStateExtrude ||
              hoverTarget === 'BASE_END_HANDLE' || hoverTarget === 'OPPOSITE_HANDLE')
            // No curser if we're drawing, or hovering over the small handles
            canvas.setCursor();
          else if (hoverTarget === 'SHAPE')
            // Grab cursor when hovering over the shape
            canvas.setCursor('move');
          else
            // Default otherwise
            canvas.setCursor('crosshair');

          if (running) requestAnimationFrame(render);
        },

        refreshPosition = function() {
          if (currentShape) {
            currentShape.anchor.canvasXY = self.imageToCanvas(currentShape.anchor.imageXY);
            currentShape.baseEnd.canvasXY = self.imageToCanvas(currentShape.baseEnd.imageXY);

            if (currentShape.opposite)
              currentShape.opposite.canvasXY = self.imageToCanvas(currentShape.opposite.imageXY);
          }
        },

        reset = function() {
          currentShape = false;
          isStateBaseline = false;
          isStateExtrude = false;
          isModifying = false;
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

    this.refreshPosition = refreshPosition;
    this.getSelection = getSelection;
    this.reset = reset;
    this.destroy = destroy;

    // Start rendering loop
    render();
  };
  TiltedBoxDrawingTool.prototype = Object.create(BaseTool.prototype);

  return TiltedBoxDrawingTool;

});
