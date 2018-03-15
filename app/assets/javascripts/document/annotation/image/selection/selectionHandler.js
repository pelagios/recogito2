define([
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/drawingCanvas'
], function(AbstractSelectionHandler, DrawingCanvas) {

    var SelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          // Keeps track of the selection currently under the mouse pointer
          currentHover = false,

          // When editing an existing selection, we keep a handle on the original
          selectionBefore = false,

          drawingCanvas = new DrawingCanvas(containerEl, olMap),

          addViewportBounds = function(selection) {
            var offset = jQuery(containerEl).offset(),
                b = selection.imageBounds, // Shorthand

                corners = [
                  [ b.left,  - b.top ],
                  [ b.right, - b.top ],
                  [ b.right, - b.bottom ],
                  [ b.left,  - b.bottom ]
                ].map(function(c) {
                  return olMap.getPixelFromCoordinate(c);
                });

            return {
              annotation: selection.annotation,
              bounds : {
                top    : corners[0][1] + offset.top,
                right  : corners[1][0] + offset.left,
                bottom : corners[2][1] + offset.top,
                left   : corners[0][0] + offset.left,
                width  : corners[1][0] - corners[0][0],
                height : corners[2][1] - corners[0][1],
              },
              imageBounds : selection.imageBounds
            };
          },

          /** @override **/
          getSelection = function() {
            return drawingCanvas.getSelection();
          },

          /**
           * Reminder: this is used to set a selection based on a URL hash.
           * @override
           */
          setSelection = function(selection) {
            selectionBefore = selection;

            if (selection) {
              // Adds viewportbounds in place - not the nicest solution...
              var withBounds = addViewportBounds(selection);
              selection.bounds = withBounds.bounds;

              // Remove from highlighter and add to drawing canvas
              highlighter.removeAnnotation(selection.annotation);
              drawingCanvas.setSelection(selection);

              self.fireEvent('select', selection);
            }
          },

          /** @override **/
          clearSelection = function() {
            if (selectionBefore) {
              // If edit was on an existing selection, move back to the highlighter and hide canvas
              highlighter.convertSelectionToAnnotation(selectionBefore);
              drawingCanvas.hide();
            } else {
              // Otherwise just reset the canvas, so we can keep drawing
              drawingCanvas.reset();
            }

            selectionBefore = false;
          },

          /** Enable the drawing tool with the given name **/
          setEnabled = function(shapeType) {
            if (shapeType) drawingCanvas.startDrawing(shapeType);
            else drawingCanvas.hide();
          },

          updateSize = function() {
            drawingCanvas.resize();
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

          onMouseClick = function(e) {
            currentHover = highlighter.getAnnotationAt(e);
            if (currentHover)
              // Click selected an existing annotation
              setSelection(currentHover);
            else
              // Deselect
              self.fireEvent('select');
          };

      olMap.on('pointermove', onMouseMove);
      olMap.on('click', onMouseClick);

      drawingCanvas.on('create', self.forwardEvent('select'));
      drawingCanvas.on('changeShape', self.forwardEvent('changeShape'));

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
