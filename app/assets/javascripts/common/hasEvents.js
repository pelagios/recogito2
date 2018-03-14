/**
 * A generic pub-sub trait.
 *
 * Objects can inherit this trait like so:
 *
 *   var MyObject = function() {
 *
 *     // Constructor - do stuff
 *
 *     HasEvents.apply(this);
 *   };
 *   MyObject.prototype = Object.create(HasEvents.prototype);
 *
 */
define(function() {

  /**
   * A simple base class that takes care of event subcription.
   * @constructor
   */
  var HasEvents = function() {
    this.handlers = {};
  };

  /**
   * Adds an event handler to this component. Refer to the docs of the components
   * for information about supported events.
   * @param {String} event the event name
   * @param {Function} handler the handler function
   */
  HasEvents.prototype.on = function(event, handler) {
    this.handlers[event] = handler;
  };

  /** Removes all event handlers from this component. **/
  HasEvents.prototype.off = function(event) {
    delete this.handlers[event];
  };

  /**
   * Fires an event.
   * @param {String} event the event name
   * @param {Object} e the event object
   * @param {Object} args the event arguments
   */
  HasEvents.prototype.fireEvent = function(event, e, args) {
    if (this.handlers[event])
      this.handlers[event](e, args);
  };

  /**
   * A helper to forward an event from a child component. Use as follows:
   *
   * childComponent.on('changed', forwardEvent('childChanged'));
   */
  HasEvents.prototype.forwardEvent = function(event) {
    var self = this;
    return function(e) {
      self.fireEvent(event, e);
    };
  };

  return HasEvents;

});
