define([
  'document/annotation/common/selection/abstractSelectionHandler'
], function(AbstractSelectionHandler) {

  var SelectionHandler = function() {

    // TODO implement

    this.getSelection = function() {};
    this.setSelection = function() {};
    this.clearSelection = function() {};

    AbstractSelectionHandler.apply(this);
  };
  SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

  return SelectionHandler;

});
