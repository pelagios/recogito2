define([
  'document/annotation/image/selection/layers/baseDrawingTool'
], function(BaseTool) {

  var pointToBounds = function(xy) {
        return { top: xy[1], right: xy[0], bottom: xy[1], left: xy[0], width: 0, height: 0 };
      };

  var PointDrawingTool = function(canvas, olMap, opt_selection) {
    BaseTool.apply(this, [ olMap ]);

    var self = this,

        mouseX, mouseY,

        // Setting this to false will stop the animation loop
        running = true,

        /**
         * Either false or an object with coordinates in canvas and image systems.
         *
         * { canvasXY: [], imageXY: [] }
         */
        currentPoint = (function() {
          if (opt_selection) {
            var b = opt_selection.imageBounds;
            return {
              canvasXY: self.imageToCanvas([ b.left, b.top ]),
              imageXY: [ b.left, b.top ]
            };
          } else {
            return false;
          }
        })(),

        isModifying = false,

        isHovering = function() {
          if (currentPoint) {
            var x = currentPoint.canvasXY[0],
                y = currentPoint.canvasXY[1];

            return mouseX <= x + BaseTool.HANDLE_RADIUS &&
              mouseX >= x - BaseTool.HANDLE_RADIUS &&
              mouseY >= y - BaseTool.HANDLE_RADIUS &&
              mouseY <= y + BaseTool.HANDLE_RADIUS;
          }
        },

        shiftPoint = function(dx, dy) {
          var cx = currentPoint.canvasXY[0] + dx,
              cy = currentPoint.canvasXY[1] + dy;

          currentPoint.canvasXY = [cx, cy];
          currentPoint.imageXY = self.canvasToImage([cx, cy]);
        },

        onMouseMove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
        },

        onMouseDown = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
          isModifying = isHovering();
        },

        onMouseClick = function(e) {
          if (!currentPoint) {
            currentPoint = {
              canvasXY: [ mouseX, mouseY ],
              imageXY: self.canvasToImage([ mouseX, mouseY ])
            };

            if (!opt_selection) self.fireEvent('create', getSelection());
          }
        },

        onMouseDrag = function(e) {
          var dx = e.originalEvent.movementX,
              dy = e.originalEvent.movementY;

          if (isModifying) {
            canvas.setForwardEvents(false);
            shiftPoint(dx, dy);
            self.fireEvent('changeShape', getSelection());
          } else {
            canvas.setForwardEvents(true);
          }
        },

        /** Refreshes the canvas position, according to the image state **/
        refreshPosition = function() {
          if (currentPoint)
            currentPoint.canvasXY = self.imageToCanvas(currentPoint.imageXY);
        },

        getSelection = function() {
          if (currentPoint) {
            var x =   Math.round(currentPoint.imageXY[0]),
                y = - Math.round(currentPoint.imageXY[1]),

                anchor = 'point:' + x + ',' + y;

            return self.buildSelection(
              anchor,
              pointToBounds(currentPoint.canvasXY),
              pointToBounds(currentPoint.imageXY));
          }
        },

        render = function() {
          var hover = isHovering();

          canvas.clear();
          if (currentPoint) self.drawHandle(canvas.ctx, currentPoint.canvasXY, { hover: hover });

          if (hover) canvas.setCursor();
          else canvas.setCursor('crosshair');

          if (running) requestAnimationFrame(render);
        },

        reset = function() {
          currentPoint = false;
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
  PointDrawingTool.prototype = Object.create(BaseTool.prototype);

  return PointDrawingTool;

});
