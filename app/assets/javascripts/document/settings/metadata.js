require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var onOrderChanged = function() {

        };

    // Make filepart elements sortable (and disable selection)
    jQuery('.part-metadata ul').disableSelection();
    jQuery('.part-metadata ul').sortable({
      stop: onOrderChanged
    });
  });

});
