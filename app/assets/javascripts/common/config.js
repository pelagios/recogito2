/**
 * A global Config object. Includes anything that's defined in window.config.
 */
define([], function() {

  var Config = {

    IS_TOUCH   : 'ontouchstart' in window || navigator.MaxTouchPoints > 0,

    hasFeature : function(name) {
      var features = this.features || [];
      return features.indexOf(name) > -1;
    }

  };

  return jQuery.extend({}, Config, window.config);

});
