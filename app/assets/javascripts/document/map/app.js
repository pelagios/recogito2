require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config'], function(AnnotationUtils, PlaceUtils, API, Config) {

  var MAX_MARKER_SIZE  = 12,

      MIN_MARKER_SIZE = 5;

  jQuery(document).ready(function() {
    var awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(document.getElementById('map'), {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 4,
          zoomControl: false,
          layers: [ awmc ]
       }),

       /** Lookuptable { gazetteerUri -> [ annotation] } **/
       annotationsByGazetteerURI = {},

       /** Keeping track of min/max values, so we can scale the dots accordingly **/
       annotationsPerPlace = { min: 9007199254740991, max : 1},

       getAnnotationsForPlace = function(place) {
         var uris = PlaceUtils.getURIs(place),
             annotations = [];

         jQuery.each(uris, function(idx, uri) {
           var annotationsForURI = annotationsByGazetteerURI[uri];
           if (annotationsForURI)
             annotations = annotations.concat(annotationsForURI);
         });
         return annotations;
       },

       renderPopup = function(coord, place) {
         var annotations = getAnnotationsForPlace(place),
             quotes = jQuery.map(annotations, function(annotation) {
               return AnnotationUtils.getQuote(annotation);
             }).join(', ');

         if (annotations.length > 0)
           L.popup().setLatLng(coord).setContent(quotes).openOn(map);
       },

       /** Size is a linear function, defined by pre-set MIN & MAX marker sizes **/
       getMarkerSize = function(place) {
         var annotationsAtPlace = getAnnotationsForPlace(place).length,
             delta = annotationsPerPlace.max - annotationsPerPlace.min,
             k = (MAX_MARKER_SIZE - MIN_MARKER_SIZE) / delta,
             d = ((MIN_MARKER_SIZE * annotationsPerPlace.max) - (MAX_MARKER_SIZE * annotationsPerPlace.min)) / delta;

         return k * annotationsAtPlace + d;
       },

       onAnnotationsLoaded = function(annotations) {
         // Loop through all place bodies of all annotations
         jQuery.each(annotations, function(i, annotation) {
           jQuery.each(AnnotationUtils.getBodiesOfType(annotation, 'PLACE'), function(j, placeBody) {
             if (placeBody.uri) {
               var annotationsAtPlace = annotationsByGazetteerURI[placeBody.uri];
               if (annotationsAtPlace)
                 annotationsAtPlace.push(annotation);
               else
                 annotationsByGazetteerURI[placeBody.uri] = [ annotation ];
             }
           });
         });

         // Determine min/max annotations per place
         jQuery.each(annotationsByGazetteerURI, function(uri, annotations) {
           var count = annotations.length;
           if (count < annotationsPerPlace.min)
             annotationsPerPlace.min = count;
           if (count > annotationsPerPlace.max)
             annotationsPerPlace.max = count;
         });

         // After the annotations are loaded, load the places
         return API.getPlacesInDocument(Config.documentId, 0, 2000);
       },

       onPlacesLoaded = function(response) {
         // TODO just a quick hack for now
         jQuery.each(response.items, function(idx, place) {
           if (place.representative_point) {
             // The epic battle between Leaflet vs. GeoJSON
             var coord = [ place.representative_point[1], place.representative_point[0] ],
                 markerSize = getMarkerSize(place);

             console.log(markerSize);

             L.circleMarker(coord).setRadius(markerSize).addTo(map).on('click', function() {
               renderPopup(coord, place);
             });
           }
         });
       },

       onLoadError = function(error) {
         // TODO implement
       };

    API.getAnnotationsForDocument(Config.documentId)
       .then(onAnnotationsLoaded)
       .done(onPlacesLoaded)
       .fail(onLoadError);
  });

});
