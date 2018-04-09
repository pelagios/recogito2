define([
  'document/annotation/image/selection/layers/placeflag/placeFlag',
  'document/annotation/image/selection/layers/baseDrawingTool',
  'document/annotation/image/selection/layers/geom2D'
], function(PlaceFlag, BaseTool, Geom2D) {

  var PlaceFlagDrawingTool = function(canvas, olMap, opt_selection) {
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
            // TODO
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
          // isModifying = getHoverTarget();
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
          canvas.clear();

          if (currentShape) {
            if (paintState === 'PIVOT')
              PlaceFlag.renderTether(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY);
            else if (paintState == 'BASELINE')
              PlaceFlag.renderBaseline(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY);
            else
              PlaceFlag.renderShape(canvas.ctx,
                currentShape.root.canvasXY,
                currentShape.pivot.canvasXY,
                currentShape.baseEnd.canvasXY,
                currentShape.opposite.canvasXY);
          }

          // Default cursor
          if (paintState)
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
    // canvas.on('drag', onMouseDrag);

    this.refreshPosition = refreshPosition;
    this.getSelection = getSelection;
    this.reset = reset;
    this.destroy = destroy;

    render();
  };
  PlaceFlagDrawingTool.prototype = Object.create(BaseTool.prototype);

  return PlaceFlagDrawingTool;

});
