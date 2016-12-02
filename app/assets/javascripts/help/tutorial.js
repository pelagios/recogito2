require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/sticky'], function(Sticky) {

  jQuery(document).ready(function() {
    var element = jQuery('.toc-wrapper'),
        maxScroll = jQuery('.page-header').outerHeight() + jQuery('.article-heading').outerHeight();

    Sticky.makeSticky(element, maxScroll);
  });

});
