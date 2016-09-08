/**
 * A base class for selectionHandler implementations on different media (text, image). Not
 * really necessary in dynamically-typed JavaScript-land, but gives a proper name to
 * things, and checks if the interface contract is (at least partially...) fulfilled.
 */
define(['common/hasEvents'], function(HasEvents) {

  var AbstractSelectionHandler = function() {

    if (!this.getSelection)
      throw 'SelectionHandler needs to implement .getSelection() method';

    if (!this.setSelection)
      throw 'SelectionHandler needs to implement .setSelection() method';

    if (!this.clearSelection)
      throw 'SelectionHandler needs to implement .clearSelection() method';

    HasEvents.apply(this);
  };
  AbstractSelectionHandler.prototype = Object.create(HasEvents.prototype);

  return AbstractSelectionHandler;

});
