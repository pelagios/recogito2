define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'],

  function(Config, AbstractSelectionHandler) {

    var PointSelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          currentSelection = false,

          mapBoundsToScreenBounds = function(mapBounds) {
            var offset = jQuery(containerEl).offset(),
                topLeft = olMap.getPixelFromCoordinate([mapBounds.left, mapBounds.top]),
                bottomRight = olMap.getPixelFromCoordinate([mapBounds.right, mapBounds.bottom]);

            return {
              top    : topLeft[1] + offset.top,
              right  : bottomRight[0] + offset.left,
              bottom : bottomRight[1] + offset.top,
              left   : topLeft[0] + offset.left,
              width  : 0,
              height : 0
            };
          },

          onClick = function(e) {
            var annotation = {
                  annotates: {
                    document_id: Config.documentId,
                    filepart_id: Config.partId,
                    content_type: Config.contentType
                  },
                  // TODO anchor
                  bodies: []
                },

                mapBounds = {
                  top    : e.coordinate[1],
                  right  : e.coordinate[0],
                  bottom : e.coordinate[1],
                  left   : e.coordinate[0],
                  width  : 0,
                  height : 0
                },

                screenBounds = mapBoundsToScreenBounds(mapBounds);

            currentSelection = { annotation: annotation, bounds: screenBounds, mapBounds : mapBounds };
            self.fireEvent('select', currentSelection);
          },

          getSelection = function() {
            // Update screenbounds, since the map may have moved
            if (currentSelection)
              currentSelection.bounds = mapBoundsToScreenBounds(currentSelection.mapBounds);
            return currentSelection;
          },

          clearSelection = function() {

          };

      olMap.on('click', onClick);

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;

      AbstractSelectionHandler.apply(this);
    };
    PointSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return PointSelectionHandler;

});
