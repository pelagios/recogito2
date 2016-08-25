define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'],

  function(Config, AbstractSelectionHandler) {

    var ToponymSelectionHandler = function(containerEl, olMap, highlighter) {
      var isEnabled = false,

          getSelection = function() {

          },

          clearSelection = function(selection) {
            
          },

          setEnabled = function(enabled) {
            isEnabled = enabled;
          };

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;

      AbstractSelectionHandler.apply(this);
    };
    ToponymSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return ToponymSelectionHandler;

});
