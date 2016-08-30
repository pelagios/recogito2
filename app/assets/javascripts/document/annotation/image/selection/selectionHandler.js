define([
  'document/annotation/common/selection/abstractSelectionHandler'
], function(AbstractSelectionHandler) {

    var SelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          currentSelection = false,

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

          /** @override **/
          getSelection = function() {
            if (currentSelection)
              return {
                annotation: currentSelection.annotation,
                mapBounds: currentSelection.mapBounds,
                bounds: mapBoundsToScreenBounds(currentSelection.mapBounds)
              };
          },

          /** @override **/
          clearSelection = function() {
            // TODO implement
          },

          /** Enable the drawing tool with the given name **/
          setEnabled = function(toolName) {
            // TODO implement
          },

          onMouseMove = function(e) {
            // TODO we may want to use this to provide mouse-hover behavior later
          },

          onClick = function(e) {
            var previousSelection = currentSelection;
            currentSelection = highlighter.getAnnotationAt(e);

            if (currentSelection) {
              // Click selected an existing annotation
              if (currentSelection !== previousSelection)
                // Selection change
                self.fireEvent('select', currentSelection);
            } else {
              // No annotation selected - activate currently active drawing tool
              console.log('start drawing - TODO');
            }
          };

      olMap.on('pointermove', onMouseMove);
      olMap.on('click', onClick);

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;

      AbstractSelectionHandler.apply(this);
    };
    SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return SelectionHandler;

});
