define([
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/layers/point/pointDrawingTool',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBoxDrawingTool'
], function(AbstractSelectionHandler, PointDrawingTool, TiltedBoxDrawingTool) {

    var SelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          currentHover = false,

          currentSelection = false,

          currentDrawingTool = false,

          drawingTools = {
            point : new PointDrawingTool(olMap),
            tbox : new TiltedBoxDrawingTool(containerEl, olMap)
          },

          attachEventHandlers = function() {
            jQuery.each(drawingTools, function(key, tool) {
              tool.on('newSelection', onNewSelection);
            });
          },

          onNewSelection = function(selection) {
            currentSelection = selection;
            self.fireEvent('select', addScreenBounds(currentSelection));
          },

          /** Converts the given map-coordinate bounds to viewport bounds **/
          mapBoundsToScreenBounds = function(mapBounds) {
            var offset = jQuery(containerEl).offset(),
                topLeft = olMap.getPixelFromCoordinate([ mapBounds.left, mapBounds.top ]),
                bottomRight = olMap.getPixelFromCoordinate([ mapBounds.right, mapBounds.bottom ]),
                bounds = {
                  top    : topLeft[1] + offset.top,
                  right  : bottomRight[0] + offset.left,
                  bottom : bottomRight[1] + offset.top,
                  left   : topLeft[0] + offset.left,
                  width  : 0,
                  height : 0
                },

                flipVertical = function() {
                  var top = bounds.top,
                      bottom = bounds.bottom;

                  bounds.top = bottom;
                  bounds.bottom = top;
                },

                flipHorizontal = function() {
                  var left = bounds.left,
                      right = bounds.right;

                  bounds.left = right;
                  bounds.right = left;
                };

            // Box might be flipped if map is rotated
            if (bounds.top > bounds.bottom)
              flipVertical();

            if (bounds.left > bounds.right)
              flipHorizontal();

            return bounds;
          },

          addScreenBounds = function(selection) {
            var clone = jQuery.extend({}, selection);
            clone.bounds = mapBoundsToScreenBounds(selection.mapBounds);
            return clone;
          },

          /** @override **/
          getSelection = function() {
            if (currentSelection)
              return addScreenBounds(currentSelection);
          },

          /** @override **/
          clearSelection = function() {
            if (currentDrawingTool)
              currentDrawingTool.clearSelection();
            currentSelection = false;
          },

          /** Enable the drawing tool with the given name **/
          setEnabled = function(toolKey) {
            var tool = (toolKey) ? drawingTools[toolKey] : false;

            if (tool != currentDrawingTool) {
              if (currentDrawingTool)
                currentDrawingTool.setEnabled(false);

              if (tool)
                tool.setEnabled(true);
            }

            currentDrawingTool = tool;
          },

          updateSize = function() {
            jQuery.each(drawingTools, function(key, tool) {
              tool.updateSize();
            });
          },

          onMouseMove = function(e) {
            var previousHover = currentHover;
            currentHover = highlighter.getAnnotationAt(e);

            if (currentHover) {
              // Mouse is over an annotation...
              if (!previousHover || currentHover.annotation !== previousHover.annotation) {
                // ...and its a new one - emphasise!
                containerEl.style.cursor = 'pointer';
                highlighter.emphasiseAnnotation(currentHover.annotation);
              }
            } else if (previousHover) {
              // Mouse changed from hover to no-hover
              containerEl.style.cursor = '';
            }
          },

          onClick = function(e) {
            var previousSelection = currentSelection;
            currentSelection = highlighter.getAnnotationAt(e);

            if (currentSelection) {
              // Click selected an existing annotation
              if (currentSelection !== previousSelection)
                // Selection change
                self.fireEvent('select', addScreenBounds(currentSelection));
            } else {
              // No annotation selected - close popup...
              self.fireEvent('select');

              // ...and activate currently active drawing tool if any
              if (currentDrawingTool)
                currentDrawingTool.createNewSelection(e);
            }
          };

      olMap.on('pointermove', onMouseMove);
      olMap.on('click', onClick);

      attachEventHandlers();

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;
      this.updateSize = updateSize;

      AbstractSelectionHandler.apply(this);
    };
    SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return SelectionHandler;

});
