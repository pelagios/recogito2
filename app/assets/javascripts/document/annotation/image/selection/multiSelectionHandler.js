define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/point/pointSelectionHandler'],

  function(Config, AbstractSelectionHandler, PointSelectionHandler) {

    var MultiSelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          selectionHandlers = {
            point : new PointSelectionHandler(containerEl, olMap, highlighter)
          },

          /** Calls the function on all selection handlers returning an array of results **/
          applyToAllHandlers = function(fnName) {
            var results = [];

            jQuery.each(selectionHandlers, function(key, selector) {
              var result = selector[fnName]();
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
          };

      registerEventHandlers();

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;

      AbstractSelectionHandler.apply(this);
    };
    MultiSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return MultiSelectionHandler;

});
