define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/drawingCanvas',
  'document/annotation/image/selection/layers/geom2D'
], function(Config, AbstractSelectionHandler, DrawingCanvas, Geom2D) {

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
                  [ b.left,  b.top ],
                  [ b.right, b.top ],
                  [ b.right, b.bottom ],
                  [ b.left,  b.bottom ]
                ].map(function(c) {
                  return olMap.getPixelFromCoordinate(c);
                }),

                bounds = Geom2D.coordsToBounds(corners);

            return {
              annotation: selection.annotation,
              bounds : {
                top    : bounds.top + offset.top,
                right  : bounds.right  + offset.left,
                bottom : bounds.bottom + offset.top,
                left   : bounds.left + offset.left,
                width  : bounds.width,
                height : bounds.height
              },
              imageBounds : selection.imageBounds
            };
          },

          /** @override **/
          getSelection = function() {
            if (Config.writeAccess)
              return drawingCanvas.getSelection();
            else if (selectionBefore)
              // In read-only mode, selection can only be an existing annotation,
              // and the drawing canvas is never in use
              return addViewportBounds(selectionBefore);
          },

          /**
           * Reminder: this is used to set a selection...
           *  i) ...based on a URL hash
           * ii) ...when clicking an existing shape
           * @override
           */
          setSelection = function(selection) {
            // console.log(selection);

            selectionBefore = selection;

            if (selection) {
              // Adds viewportbounds in place - not the nicest solution...
              var withBounds = addViewportBounds(selection);
              selection.bounds = withBounds.bounds;

              // Remove from highlighter and add to drawing canvas
              if (Config.writeAccess) {
                highlighter.removeAnnotation(selection.annotation);
                drawingCanvas.setSelection(selection);
              }

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
            if (shapeType) {
              drawingCanvas.startDrawing(shapeType);
            } else {
              if (selectionBefore) highlighter.convertSelectionToAnnotation(selectionBefore);
              drawingCanvas.hide();
            }
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
