require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/plugins'
], function(Plugins) {

  jQuery(document).ready(function() {
    Plugins.init();
  });

});
