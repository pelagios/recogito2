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

          /** Registers handlers to forward 'select' events from child selection handler **/
          registerEventHandlers = function() {
            jQuery.each(selectionHandlers, function(key, handler) {
              handler.on('select', function(e) { self.fireEvent('select', e); });
            });
          },

          getSelection = function() {
            var selections = applyToAllHandlers('getSelection');
            if (selections.length > 0)
              return selections[0]; // TODO can we have multiple selections in the future?
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
