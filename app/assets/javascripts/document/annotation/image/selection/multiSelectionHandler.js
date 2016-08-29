define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/point/pointSelectionHandler',
  'document/annotation/image/selection/toponym/toponymSelectionHandler'
], function(
    Config,
    AbstractSelectionHandler,
    PointSelectionHandler,
    ToponymSelectionHandler) {

    var MultiSelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          selectionHandlers = {
            point   : new PointSelectionHandler(containerEl, olMap, highlighter),
            toponym : new ToponymSelectionHandler(containerEl, olMap, highlighter)
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

          /** Calls the function on all selection handlers returning an array of results **/
          applyToAllHandlers = function(fnName, arg1) {
            var results = [];

            jQuery.each(selectionHandlers, function(key, selector) {
              var result = selector[fnName](arg1);
              if (result)
                results.push(result);
            });

            return results;
          },

          addScreenBounds = function(selection) {
            // TODO mutates the selection...
            // TODO but still seems simplest solution to re-use map->screen conversion
            selection.bounds = mapBoundsToScreenBounds(selection.mapBounds);
            return selection;
          },

          onSelect = function(selection) {
            self.fireEvent('select', addScreenBounds(selection));
          },

          /** Registers handlers to forward 'select' events from child selection handler **/
          registerEventHandlers = function() {
            jQuery.each(selectionHandlers, function(key, handler) {
              handler.on('select', onSelect);
            });
          },

          getSelection = function() {
            var selections = applyToAllHandlers('getSelection');
            if (selections.length > 0) {
              return addScreenBounds(selections[0]); // TODO can we have multiple selections in the future?
            }
          },

          clearSelection = function() {
            applyToAllHandlers('clearSelection');
          },

          setEnabled = function(toolName) {
            // Disable all
            applyToAllHandlers('setEnabled', false);

            if (toolName) {
              var handler = selectionHandlers[toolName.toLowerCase()];
              if (handler)
                handler.setEnabled(true);
            }
          };

      registerEventHandlers();

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;

      AbstractSelectionHandler.apply(this);
    };
    MultiSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return MultiSelectionHandler;

});
