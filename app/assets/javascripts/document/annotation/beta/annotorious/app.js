require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: {
    marked: '/webjars/marked/0.3.6/marked.min',
    i18n: '../vendor/i18n'
  }
});

require([], function() {

    /** On page load, fetch the manifest and instantiate the app **/
    jQuery(document).ready(function() {

      var activeImageLoaded = false;

      new Blazy({ // Init image lazy loading lib
        offset: 0,
        container: '.sidebar .menu',
        validateDelay: 200,
        saveViewportOffsetDelay: 200,
        success: function(element) {
          var li = element.parentNode.parentNode;

          if (li.className == 'active')
            activeImageLoaded = true;

          if (!activeImageLoaded)
            document.querySelector('.sidebar li.active').scrollIntoView(true);
        }
      });
    });

});
