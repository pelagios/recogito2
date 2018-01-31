require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/annotationView',
  'common/api',
  'common/config',
  'document/map/map'
], function(AnnotationView, API, Config, Map) {

  jQuery(document).ready(function() {

    var map = new Map(jQuery('.map')),

        /** Init the map with the annotations and fetch places **/
        onAnnotationsLoaded = function(a) {
          var annotations = new AnnotationView(a);
          map.setAnnotations(annotations.readOnly());
          return API.listPlacesInDocument(Config.documentId, 0, 2000);
        },

        /** Init the map with the places **/
        onPlacesLoaded = function(response) {
          map.setPlaces(response.items);
        },

        onLoadError = function(error) {
          // TODO implement
        };

    API.listAnnotationsInDocument(Config.documentId)
       .then(onAnnotationsLoaded)
       .done(onPlacesLoaded)
       .fail(onLoadError);
  });

});
