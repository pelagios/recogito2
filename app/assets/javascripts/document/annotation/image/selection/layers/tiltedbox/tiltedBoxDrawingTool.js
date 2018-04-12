define([
  'document/annotation/image/selection/layers/tiltedbox/tiltedBox',
  'document/annotation/image/selection/layers/baseDrawingTool',
  'document/annotation/image/selection/layers/geom2D'
], function(TiltedBox, BaseTool, Geom2D) {

  var TiltedBoxDrawingTool = function(canvas, olMap, opt_selection) {
    // Extend at start, so that we have base prototype methods available on init (currentShape!)
    BaseTool.apply(this, [ olMap ]);

    var self = this,

        mouseX, mouseY,

        /**
         * Either false or an object of the following form:
         *
         * {
         *   pivot: { canvasXY: [], imageXY: [] },
         *   baseEnd: { canvasXY: [], imageXY: [] },
         *   opposite: { canvasXY: [], imageXY: [] }
         * }
         */
        currentShape = (function() {
          if (opt_selection) {
            var coords = TiltedBox.parseAnchor(opt_selection.annotation.anchor).coords;
            return {
              pivot:   { canvasXY: self.imageToCanvas(coords[0]), imageXY: coords[0] },
              baseEnd:  { canvasXY: self.imageToCanvas(coords[1]), imageXY: coords[1] },
              opposite: { canvasXY: self.imageToCanvas(coords[2]), imageXY: coords[2] }
            };
          } else {
            return false;
          }
        })(),

        // Painting state flags
        isStateBaseline = false,
        isStateExtrude = false,

        isModifying = false,

        // Setting this to false will stop the animation loop
        running = true,

        updateShape = function(diff) {
          // Perform deep clone
          currentShape = (currentShape) ? jQuery.extend(true, {}, currentShape, diff) : diff;
          if (diff.pivot) self.crossFill(diff.pivot, currentShape.pivot, 'canvasXY', 'imageXY');
          if (diff.baseEnd) self.crossFill(diff.baseEnd, currentShape.baseEnd, 'canvasXY', 'imageXY');
          if (diff.opposite) self.crossFill(diff.opposite, currentShape.opposite, 'canvasXY', 'imageXY');
        },

        shiftCoord = function(coord, dx, dy) {
          return { canvasXY: [ coord.canvasXY[0] + dx, coord.canvasXY[1] + dy ] };
        },

        shiftShape = function(dx, dy) {
          updateShape({
            pivot   : shiftCoord(currentShape.pivot, dx, dy),
            baseEnd  : shiftCoord(currentShape.baseEnd, dx, dy),
            opposite : shiftCoord(currentShape.opposite, dx, dy)
          });
        },

        shiftBaseline = function(dx, dy) {
          var pivot = currentShape.pivot.canvasXY,
              baseEnd = currentShape.baseEnd.canvasXY,
              opposite = currentShape.opposite.canvasXY,
              h = Geom2D.len(opposite[0], opposite[1], baseEnd[0], baseEnd[1]),

              angle =
                Geom2D.angleOf(Geom2D.vec(pivot, baseEnd)) -
                Geom2D.angleOf(Geom2D.vec(pivot, opposite)),

              orientation = (angle > 0) ? 1 : -1;

          updateShape({ baseEnd: shiftCoord(currentShape.baseEnd, dx, dy) });

          var baseLineAfter = getCanvasBaseline(),
              normalAfter = Geom2D.normalize([ orientation * baseLineAfter[1], - orientation * baseLineAfter[0] ]),
              dOpposite = [ normalAfter[0] * h, normalAfter[1] * h ];

          updateShape({ opposite: {
            canvasXY: [
              currentShape.baseEnd.canvasXY[0] + dOpposite[0],
              currentShape.baseEnd.canvasXY[1] + dOpposite[1]
            ]
          }});
        },

        shiftOpposite = function() {
          updateShape({ opposite: { canvasXY: getFloatingOpposite() }});
        },

        /** Computes opposite corner corresponding to the current mouse position **/
        getFloatingOpposite = function() {
          var baseEnd = currentShape.baseEnd.canvasXY,
              baseline = getCanvasBaseline(),
              normal = Geom2D.normalize([ - baseline[1], baseline[0] ]),

              // Vector baseline -> mouse
              toMouse = [ mouseX - baseEnd[0], mouseY - baseEnd[1] ],

              // Projection of toMouse onto normal
              f = [
                normal[0] * Geom2D.len(toMouse) * Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse))),
                normal[1] * Geom2D.len(toMouse) * Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse)))
              ];

          return [ baseEnd[0] + f[0], baseEnd[1] + f[1] ];
        },

        /** Shorthand to get the baseline vector from the current shape **/
        getCanvasBaseline = function() {
          var pivot = currentShape.pivot.canvasXY,
              baseEnd = currentShape.baseEnd.canvasXY;
          return [ baseEnd[0] - pivot[0], baseEnd[1] - pivot[1] ];
        },

        /** Shorthand to get the box coordinates of the current shape in canvas coord space **/
        getShapeCanvasCoords = function() {
          var pivot    = currentShape.pivot.canvasXY,
              baseEnd  = currentShape.baseEnd.canvasXY,
              opposite = currentShape.opposite.canvasXY,

              // Vector baseEnd -> opposite
              fx = opposite[0] - baseEnd[0],
              fy = opposite[1] - baseEnd[1];

          // Coordinates in clockwise direction
          return [ pivot, [ pivot[0] + fx, pivot[1] + fy ], opposite, baseEnd ];
        },

        /** Shorthand to get the box coordinates of the current shape in image coord space **/
        getImageCanvasCoords = function() {
          var pivot    = currentShape.pivot.imageXY,
              baseEnd  = currentShape.baseEnd.imageXY,
              opposite = currentShape.opposite.imageXY,

              // Vector baseEnd -> opposite
              fx = opposite[0] - baseEnd[0],
              fy = opposite[1] - baseEnd[1];

          // Coordinates in clockwise direction
          return [ pivot, [ pivot[0] + fx, pivot[1] + fy ], opposite, baseEnd ];
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
            else if (isOverHandle(currentShape.pivot, BaseTool.HANDLE_RADIUS))
              return 'SHAPE';
            else if (isOverHandle(currentShape.baseEnd, TiltedBox.SMALL_HANDLE_RADIUS))
              return 'BASE_END_HANDLE';
            else if (isOverHandle(currentShape.opposite, TiltedBox.SMALL_HANDLE_RADIUS))
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

        onMouseUp = function() {
          isModifying = false;
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
              pivot: { canvasXY: [ mouseX, mouseY ] },
              baseEnd: { canvasXY: [ mouseX, mouseY ]}
            });
          }
        },

        onMouseDrag = function(e) {
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY,

              // Don't drag the background map if the shape is being modfied
              dontDrag = isModifying || isStateBaseline || isStateExtrude;

          if (dontDrag) canvas.setForwardEvents(false);
          else canvas.setForwardEvents(true);

          if (isModifying === 'SHAPE')
            shiftShape(dx, dy);
          else if (isModifying === 'BASE_END_HANDLE')
            shiftBaseline(dx, dy);
          else if (isModifying === 'OPPOSITE_HANDLE')
            shiftOpposite();

          // If it's a modification, fire changeShape event
          if (isModifying) self.fireEvent('changeShape', getSelection());
        },

        getSelection = function() {
          if (currentShape) {
            var canvasBounds = Geom2D.coordsToBounds(getShapeCanvasCoords()),
                imageBounds = Geom2D.coordsToBounds(getImageCanvasCoords()),
                anchor = TiltedBox.getAnchor(
                  currentShape.pivot.imageXY,
                  currentShape.baseEnd.imageXY,
                  currentShape.opposite.imageXY);

            return self.buildSelection(anchor, canvasBounds, imageBounds);
          }
        },

        render = function() {
          var highlight;
          if (isStateBaseline)
            highlight = 'BASE_END_HANDLE';
          else if (isStateExtrude)
            highlight = 'OPPOSITE_HANDLE';
          else if (isModifying)
            highlight = isModifying;
          else
            highlight = getHoverTarget();

          canvas.clear();
          if (currentShape)
            if (currentShape.opposite) // Finished shape
              TiltedBox.renderShape(canvas.ctx,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY,
                currentShape.opposite.canvasXY,
                highlight);
            else if (isStateExtrude)
              TiltedBox.renderShape(canvas.ctx,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY,
                getFloatingOpposite(),
                highlight);
            else if (isStateBaseline)
              TiltedBox.renderBaseline(canvas.ctx,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY,
                highlight);

          // if (currentShape) drawCurrentShape(canvas.ctx, hoverTarget);

          if (highlight === 'BASE_END_HANDLE' || highlight === 'OPPOSITE_HANDLE')
            // No curser if we're drawing, or hovering over the small handles
            canvas.setCursor();
          else if (highlight === 'SHAPE')
            // Grab cursor when hovering over the shape
            canvas.setCursor('move');
          else
            // Default otherwise
            canvas.setCursor('crosshair');

          if (running) requestAnimationFrame(render);
        },

        refreshPosition = function() {
          if (currentShape) {
            currentShape.pivot.canvasXY = self.imageToCanvas(currentShape.pivot.imageXY);
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
    canvas.on('mouseup', onMouseUp);
    canvas.on('click', onMouseClick);
    canvas.on('drag', onMouseDrag);

    this.refreshPosition = refreshPosition;
    this.getSelection = getSelection;
    this.reset = reset;
    this.destroy = destroy;

    render();
  };
  TiltedBoxDrawingTool.prototype = Object.create(BaseTool.prototype);

  return TiltedBoxDrawingTool;

});
