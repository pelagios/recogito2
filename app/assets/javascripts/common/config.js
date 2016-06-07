/**
 * A global Config object. Includes anything that's defined in window.config.
 */
define([], function() {

  var Config = {

    IS_TOUCH : 'ontouchstart' in window || navigator.MaxTouchPoints > 0,

  };

  return jQuery.extend({}, Config, window.config);

});
