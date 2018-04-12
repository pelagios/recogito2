define([
  'document/annotation/image/selection/layers/linkedbox/linkedBox',
  'document/annotation/image/selection/layers/baseDrawingTool',
  'document/annotation/image/selection/layers/geom2D'
], function(LinkedBox, BaseTool, Geom2D) {

  var pointToBounds = function(xy) {
        return { top: xy[1], right: xy[0], bottom: xy[1], left: xy[0], width: 0, height: 0 };
      };

  var LinkedBoxDrawingTool = function(canvas, olMap, opt_selection) {
    // Extends the tilted box tool
    BaseTool.apply(this, [ olMap ]);

    var self = this,

        mouseX, mouseY,

        /**
         * Either false or an object of the following form:
         *
         * {
         *   root: { canvasXY: [], imageXY [] },
         *   pivot: { canvasXY: [], imageXY: [] },
         *   baseEnd: { canvasXY: [], imageXY: [] },
         *   opposite: { canvasXY: [], imageXY: [] }
         * }
         */
        currentShape = (function() {
          if (opt_selection) {
            var anchor = LinkedBox.parseAnchor(opt_selection.annotation.anchor),
                root = [ anchor.rx, - anchor.ry ],
                coords = anchor.coords;

            return {
              root: { canvasXY: self.imageToCanvas(root), imageXY: root },
              pivot:   { canvasXY: self.imageToCanvas(coords[0]), imageXY: coords[0] },
              baseEnd:  { canvasXY: self.imageToCanvas(coords[1]), imageXY: coords[1] },
              opposite: { canvasXY: self.imageToCanvas(coords[2]), imageXY: coords[2] }
            };
          } else {
            return false;
          }
        })(),

        // PIVOT, BASELINE, EXTRUDE or false
        paintState = false,

        isModifying = false,

        // Setting this to false will stop the animation loop
        running = true,

        updateShape = function(diff) {
          var crossFill = function(props, shape) {
                props.forEach(function(prop) {
                  if (diff[prop])
                    self.crossFill(diff[prop], shape[prop], 'canvasXY', 'imageXY');
                });
              };
          // Perform deep clone
          currentShape = (currentShape) ? jQuery.extend(true, {}, currentShape, diff) : diff;
          crossFill(['root', 'pivot', 'baseEnd', 'opposite'], currentShape);
        },

        /** Shorthand to get the baseline vector from the current shape **/
        getCanvasBaseline = function() {
          var pivot = currentShape.pivot.canvasXY,
              baseEnd = currentShape.baseEnd.canvasXY;
          return [ baseEnd[0] - pivot[0], baseEnd[1] - pivot[1] ];
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

        getBoxCanvasCoords = function() {
          var pivot    = currentShape.pivot.canvasXY,
              baseEnd  = currentShape.baseEnd.canvasXY,
              opposite = currentShape.opposite.canvasXY,

              // Vector baseEnd -> opposite
              fx = opposite[0] - baseEnd[0],
              fy = opposite[1] - baseEnd[1];

          // Coordinates in clockwise direction
          return [ pivot, [ pivot[0] + fx, pivot[1] + fy ], opposite, baseEnd ];
        },

        shiftCoord = function(coord, dx, dy) {
          return { canvasXY: [ coord.canvasXY[0] + dx, coord.canvasXY[1] + dy ] };
        },

        shiftRoot = function(dx, dy) {
          updateShape({ root: shiftCoord(currentShape.root, dx, dy) });
        },

        shiftShape = function(dx, dy) {
          // TODO lots of redundancy removal should be possible...
          updateShape({
            pivot: shiftCoord(currentShape.pivot, dx, dy),
            baseEnd: shiftCoord(currentShape.baseEnd, dx, dy),
            opposite: shiftCoord(currentShape.opposite, dx, dy)
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

        getHoverTarget = function() {
          var isOverHandle = function(coord, radius) {
                if (coord) {
                  var x = coord.canvasXY[0], y = coord.canvasXY[1];
                  return mouseX <= x + radius && mouseX >= x - radius &&
                         mouseY >= y - radius && mouseY <= y + radius;
                }
              };

          if (currentShape && currentShape.opposite)
            if (isOverHandle(currentShape.root, BaseTool.HANDLE_RADIUS))
              return 'ROOT';
            else if (Geom2D.intersects(mouseX, mouseY, getBoxCanvasCoords()))
              return 'SHAPE';
            else if (isOverHandle(currentShape.pivot, 6))
              return 'SHAPE';
            else if (isOverHandle(currentShape.baseEnd, 6))
              return 'BASE_END_HANDLE';
            else if (isOverHandle(currentShape.opposite, 6))
              return 'OPPOSITE_HANDLE';
        },

        onMouseMove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;

          if (paintState === 'PIVOT')
            updateShape({ pivot: { canvasXY: [ mouseX, mouseY ] } });
          else if (paintState === 'BASELINE')
            updateShape({ baseEnd: { canvasXY: [ mouseX, mouseY ] } });
          else if (paintState === 'OPPOSITE')
            updateShape({ opposite: { canvasXY: getFloatingOpposite() } });
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
            if (paintState === 'PIVOT') {
              // Fix the Pivot and start defining the baseline
              paintState = 'BASELINE';
              updateShape({ baseEnd: { canvasXY: [ mouseX, mouseY ] } });
            } else if (paintState === 'BASELINE') {
              // Fix the baseline and start defining height
              paintState = 'OPPOSITE';
              updateShape({ opposite: { canvasXY: [ mouseX, mouseY ] } });
            } else {
              // Fix opposite - and done
              paintState = false;
              self.fireEvent('create', getSelection());
            }
          } else {
            // Start new shape
            paintState = 'PIVOT';
            updateShape({
              root: { canvasXY: [ mouseX, mouseY ] },
              pivot: { canvasXY: [ mouseX, mouseY ]}
            });
          }
        },

        onMouseDrag = function(e) {
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          // Don't drag the background map if the shape is being modfied
          if (isModifying || paintState) canvas.setForwardEvents(false);
          else canvas.setForwardEvents(true);


          if (isModifying === 'ROOT')
            shiftRoot(dx, dy);
          else if (isModifying === 'SHAPE')
            shiftShape(dx, dy);
          else if (isModifying === 'BASE_END_HANDLE')
            shiftBaseline(dx, dy);
          else if (isModifying === 'OPPOSITE_HANDLE')
            shiftOpposite();

          // If it's a modification, fire changeShape event
          if (isModifying) self.fireEvent('changeShape', getSelection());
        },

        refreshPosition = function() {
          if (currentShape) {
            currentShape.root.canvasXY = self.imageToCanvas(currentShape.root.imageXY);
            currentShape.pivot.canvasXY = self.imageToCanvas(currentShape.pivot.imageXY);
            if (currentShape.baseEnd)
              currentShape.baseEnd.canvasXY = self.imageToCanvas(currentShape.baseEnd.imageXY);
            if (currentShape.opposite)
              currentShape.opposite.canvasXY = self.imageToCanvas(currentShape.opposite.imageXY);
          }
        },

        getSelection = function() {
          if (currentShape) {
            return self.buildSelection(
              LinkedBox.getAnchor(
                currentShape.root.imageXY,
                currentShape.pivot.imageXY,
                currentShape.baseEnd.imageXY,
                currentShape.opposite.imageXY),
              pointToBounds(currentShape.root.canvasXY),
              pointToBounds(currentShape.root.imageXY));
          }
        },

        reset = function() {
          currentShape = false;
          paintState = false;
        },

        destroy = function() {
          running = false;
          canvas.off('mousemove');
          canvas.off('mousedown');
          canvas.off('click');
          canvas.off('drag');
        };

        render = function() {
          var hoverTarget = isModifying || getHoverTarget();

          canvas.clear();

          if (currentShape) {
            if (paintState === 'PIVOT')
              LinkedBox.renderTether(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY);
            else if (paintState == 'BASELINE')
              LinkedBox.renderBaseline(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY);
            else
              LinkedBox.renderShape(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY,
                currentShape.opposite.canvasXY,
                hoverTarget);
          }

          // Default cursor
          if (hoverTarget === 'SHAPE')
            canvas.setCursor('move');
          else if (paintState || hoverTarget)
            canvas.setCursor();
          else
            canvas.setCursor('crosshair');

          if (running) requestAnimationFrame(render);
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
  LinkedBoxDrawingTool.prototype = Object.create(BaseTool.prototype);

  return LinkedBoxDrawingTool;

});
