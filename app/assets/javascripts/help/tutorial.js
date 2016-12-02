require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/behavior'], function(Behavior) {

  jQuery(document).ready(function() {
    var element = jQuery('.toc-wrapper'),
        maxScroll = jQuery('.page-header').outerHeight() + jQuery('.article-heading').outerHeight();

    Behavior.makeElementSticky(element, maxScroll);
    Behavior.animateAnchorNav(element);
  });

});
