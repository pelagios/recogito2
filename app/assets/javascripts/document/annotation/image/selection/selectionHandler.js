define([
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/layers/point/pointDrawingTool',
  'document/annotation/image/selection/layers/rect/rectDrawingTool',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBoxDrawingTool'
], function(AbstractSelectionHandler, PointDrawingTool, RectDrawingTool, TiltedBoxDrawingTool) {

    var SelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          currentHover = false,

          currentSelection = false,

          currentDrawingTool = false,

          drawingTools = {
            point : new PointDrawingTool(olMap),
            rect  : new RectDrawingTool(olMap),
            tbox  : new TiltedBoxDrawingTool(containerEl, olMap)
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

                pxBounds = mapBounds.map(function(coord) {
                  return olMap.getPixelFromCoordinate([ coord.x, - coord.y ]);
                }),

                bbox = (function() {
                  var x = pxBounds.map(function(px) { return px[0]; }),
                      y = pxBounds.map(function(px) { return px[1]; }),

                      top = Math.min.apply(null, y) + offset.top ,
                      right = Math.max.apply(null, x) + offset.left,
                      bottom = Math.max.apply(null, y) + offset.top,
                      left = Math.min.apply(null, x) + offset.left;

                  return {
                    top    : top,
                    right  : right,
                    bottom : bottom,
                    left   : left,
                    width  : right - left,
                    height : bottom - top
                  };
                })();

            return bbox;
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
          setSelection = function(selection) {
            currentSelection = selection;
            if (selection)
              self.fireEvent('select', addScreenBounds(currentSelection));
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
            if (currentDrawingTool) {
              // Just forward to the drawing tool
              currentDrawingTool.createNewSelection(e);
            } else {
              var previousSelection = currentSelection;
              currentSelection = highlighter.getAnnotationAt(e);

              if (currentSelection) {
                // Click selected an existing annotation
                if (currentSelection !== previousSelection)
                  // Selection change
                  self.fireEvent('select', addScreenBounds(currentSelection));
              } else {
                // No annotation - deselect
                self.fireEvent('select');
              }
            }
          };

      olMap.on('pointermove', onMouseMove);
      olMap.on('click', onClick);

      attachEventHandlers();

      this.getSelection = getSelection;
      this.setSelection = setSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;
      this.updateSize = updateSize;

      AbstractSelectionHandler.apply(this);
    };
    SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return SelectionHandler;

});
