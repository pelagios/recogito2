define([
  'common/hasEvents',
  'document/annotation/image/selection/layers/point/pointDrawingTool',
  'document/annotation/image/selection/layers/rect/rectDrawingTool',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBoxDrawingTool'
], function(HasEvents, PointDrawingTool, RectDrawingTool, TiltedBoxDrawingTool) {

  var ZOOM_DURATION = 250,
      MIN_DRAG_TIME = 150; // Everything below that threshold = click, otherwise drag

      TOOLS = {
        // point : new PointDrawingTool(olMap),
        rect  : RectDrawingTool
        // tbox  : new TiltedBoxDrawingTool(containerEl, olMap)
      };

  var DrawingCanvas = function(containerEl, olMap) {

    var self = this,

        canvas = jQuery('<canvas class="drawing-canvas"></canvas>')
          .hide()
          .appendTo(containerEl),

        olView = olMap.getView(),

        // Tracks mouse state
        isMouseDown = false,
        lastClickTime,

        // Tracks key state (rotation)
        keysHeld = { shift: false, alt: false },

        // Event forwarding state: if true, mouse events are forwarded to OpenLayers
        forwardEvents = true,

        // The currently active drawing tool (if any)
        currentDrawingTool = false,

        /**
         * Attach mouse and key event handlers, init the canvas size
         * and remove cursor (the drawing tools will provide it).
         */
        init = function() {
          canvas.mousemove(onMouseMove);
          canvas.mousedown(onMouseDown);
          canvas.mouseup(onMouseUp);
          canvas.bind('wheel', onMouseWheel);

          jQuery(document).keydown(onKeyDown);
          jQuery(document).keyup(onKeyUp);

          resize();

          setCursor();
        },

        /**
         * Re-initializes the canvas properties based on the current container size.
         */
        resize = function() {
          canvas.attr('width', canvas.width());
          canvas.attr('height', canvas.height());

          self.ctx = canvas.get(0).getContext('2d');
          self.width = canvas.get(0).width;
          self.height = canvas.get(0).height;
          self.center = [ self.width / 2, self.height / 2 ];
          self.offset = jQuery(containerEl).offset();
        },

        /**
         * Utility function that converts the bounds of a selection from
         * canvas coordinate space to viewport coordinate space.
         */
        toViewportSelection = function(selection) {
          return {
            isNew: true, // For compatiblity with text mode
            annotation: selection.annotation,
            bounds : {
              top    : selection.canvasBounds.top + self.offset.top,
              right  : selection.canvasBounds.right + self.offset.left,
              bottom : selection.canvasBounds.bottom + self.offset.top,
              left   : selection.canvasBounds.left + self.offset.left,
              width  : selection.canvasBounds.width,
              height : selection.canvasBounds.height
            },
            origBounds : selection.origBounds
          };
        },

        /**
         * Returns the current selection if any.
         */
        getSelection = function() {
          var currentSelection = (currentDrawingTool) ? currentDrawingTool.getSelection() : false;
          if (currentSelection)
            return toViewportSelection(currentSelection);
        },

        /**
         * Opens the given selection in the appropriate drawing tool.
         */
        setSelection = function(selection) {
          // TODO branch based on anchor type
          startDrawing('rect', selection);
        },

        /**
         * Starts drawing with the appropriate tool for the given shape type.
         * Optionally, on an existing selection (otherwise on a new shape).
         */
        startDrawing = function(shapeType, opt_selection) {
          var toolFn = (shapeType) ? TOOLS[shapeType] : false,

              // Shorthand for forwarding create/modify events with added viewport offset
              forwardWithViewport = function(event) {
                return function(selection) {
                  self.fireEvent(event, toViewportSelection(selection));
                };
              };

          if (currentDrawingTool)
            currentDrawingTool.destroy();

          if (toolFn) {
            currentDrawingTool = new toolFn(self, olMap, opt_selection);

            currentDrawingTool.on('create', forwardWithViewport('create'));
            currentDrawingTool.on('changeShape', forwardWithViewport('changeShape'));

            canvas.show();
          }
        },

        /**
         * Clears the canvas and resets the current drawing tool (if any), so we can
         * start drawing a new selection.
         */
        reset = function() {
          if (currentDrawingTool) currentDrawingTool.reset();
        },

        /**
         * De-activates the canvas by destroying the current drawing tool instance
         * and hiding the canvas element.
         */
        hide = function() {
          if (currentDrawingTool)
            currentDrawingTool.destroy();
          canvas.hide();
        },

        /** Clears the canvas **/
        clear = function() {
          self.ctx.clearRect(0, 0, self.width, self.height);
        },

        /**
         * Determines wether the event is a plain mousemove or a drag event.
         * If the move is also a drag, the event is also and forwarded to OpenLayers,
         * if forwarding is enabled required.
         */
        onMouseMove = function(e) {
          var forwardMouseMove = function() {
                var dx = e.originalEvent.movementX,
                    dy = e.originalEvent.movementY;

                    destView = [ self.center[0] - dx, self.center[1] - dy ];
                    destMap = olMap.getCoordinateFromPixel(destView);

                olView.setCenter(destMap);
              };

          self.fireEvent('mousemove', e);

          if (isMouseDown) {
            self.fireEvent('drag', e);
            if (forwardEvents) forwardMouseMove();
          }
        },

        onMouseDown = function(e) {
          isMouseDown = true;
          lastClickTime = new Date().getTime();
          self.fireEvent('mousedown', e);
        },

        onMouseUp = function(e) {
          isMouseDown = false;
          var now = new Date().getTime();
          if ((now - lastClickTime) < MIN_DRAG_TIME)
            self.fireEvent('click', e);
        },

        /** Mouse wheel events are always forwarded to OpenLayers **/
        onMouseWheel = function(e) {
          var delta = e.originalEvent.deltaY,
              dir = delta / Math.abs(delta);

          olView.animate({ zoom: olView.getZoom() - dir, duration: ZOOM_DURATION });
        },

        onKeyDown = function(e) {
          if (e.which === 16) keysHeld.shift = true; // SHIFT
          if (e.which == 18) keysHeld.alt = true; // ALT

          // If SHIFT+ALT make transparent to mouse events
          if (keysHeld.alt && keysHeld.shift) canvas.css('pointer-events', 'none');
        },

        onKeyUp = function(e) {
          if (e.which === 16) keysHeld.shift = false; // SHIFT
          if (e.which == 18) keysHeld.alt = false; // ALT

          // If SHIFT+ALT switch back to capturing mouse events
          if (!(keysHeld.alt || keysHeld.shift)) canvas.css('pointer-events', 'auto');
        },

        setForwardEvents = function(enabled) {
          forwardEvents = enabled;
        },

        setCursor = function(cursor) {
          if (cursor) canvas.css('cursor', cursor);
          else canvas.css('cursor', 'none');
        };

    // Reset canvas on window resize
    jQuery(window).on('resize', resize);

    // Used by the selectionHandler
    this.getSelection = getSelection;
    this.setSelection = setSelection;
    this.startDrawing = startDrawing;
    this.hide = hide;
    this.reset = reset;
    this.resize = resize;

    // Used by the drawing tools
    this.setForwardEvents = setForwardEvents;
    this.setCursor = setCursor;
    this.clear = clear;

    // Properties initialized on .init and .resize
    this.ctx = undefined;
    this.width = undefined;
    this.height = undefined;
    this.center = undefined;
    this.offset = undefined;

    init();

    HasEvents.apply(this);
  };
  DrawingCanvas.prototype = Object.create(HasEvents.prototype);

  return DrawingCanvas;

});
