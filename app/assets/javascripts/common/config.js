/**
 * A global Config object. Includes anything that's defined in window.config.
 */
define([], function() {

  var Config = {

    IS_TOUCH : 'ontouchstart' in document.documentElement

  };

  return jQuery.extend({}, Config, window.config);

});
