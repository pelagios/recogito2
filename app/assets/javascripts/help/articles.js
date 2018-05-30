require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/behavior'], function(Behavior) {

  jQuery(document).ready(function() {
    Behavior.makeAnchorsClickable();
  });

});
