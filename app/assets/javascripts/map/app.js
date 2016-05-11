require([
  '../common/annotationUtils',
  '../common/apiConnector',
  '../common/config',
  '../common/placeUtils',], function(AnnotationUtils, API, Config, PlaceUtils) {

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

       onAnnotationsLoaded = function(annotations) {
         // Loop through all place bodies of all annotations
         jQuery.each(annotations, function(i, annotation) {
           jQuery.each(AnnotationUtils.getBodiesOfType(annotation, 'PLACE'), function(j, placeBody) {
             var annnotationsAtPlace = annotationsByGazetteerURI[placeBody.uri];
             if (annnotationsAtPlace)
               annotationsAtPlace.push(annotation);
             else
               annotationsByGazetteerURI[placeBody.uri] = [ annotation ];
           });
         });
       },

       onPlacesLoaded = function(places) {
         // TODO just a quick hack for now
         jQuery.each(places, function(idx, place) {
           if (place.representative_point) {
             // The epic battle between Leaflet vs. GeoJSON
             var coord = [ place.representative_point[1], place.representative_point[0] ];
             L.marker(coord).addTo(map).on('click', function() {
               renderPopup(coord, place);
             });
           }
         });
       },

       onLoadError = function(error) {
         // TODO implement
       };

    API.getAnnotationsForDocument(Config.documentId)
       .done(onAnnotationsLoaded)
       .fail(onLoadError);

    API.getPlacesInDocument(Config.documentId)
       .done(onPlacesLoaded)
       .fail(onLoadError);
  });

});
