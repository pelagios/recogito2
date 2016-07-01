require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config'], function(Formatting, AnnotationUtils, PlaceUtils, API, Config) {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4,

      FILL_COLOR = '#31a354',

      STROKE_COLOR = '#006d2c';

  jQuery(document).ready(function() {
    var awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(jQuery('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 4,
          zoomControl: false,
          layers: [ awmc ]
       }),

       /** Lookup table { gazetteerUri -> [ annotation] } **/
       annotationsByGazetteerURI = {},

       markerScaleFn,

       distinct = function(arr) {
         var distinct = [];
         jQuery.each(arr, function(idx, elem) {
           if (distinct.indexOf(elem) < 0)
             distinct.push(elem);
         });
         return distinct;
       },

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

       /** Extracts the list of distinct URIs referenced in the given annotations **/
       getDistinctURIs = function(annotations) {
         var distinctURIs = [];
         jQuery.each(annotations, function(i, a) {
           var placeBodies = AnnotationUtils.getBodiesOfType(a, 'PLACE');
           jQuery.each(placeBodies, function(j, b) {
             if (b.uri && distinctURIs.indexOf(b.uri) < 0)
                 distinctURIs.push(b.uri);
           });
         });
         return distinctURIs;
       },

       renderPopup = function(coord, place) {
             // TODO sort distinct quotes by frequency
         var annotations = getAnnotationsForPlace(place),

             currentSnippet = 0,

             quotes = jQuery.map(annotations, function(annotation) {
               return AnnotationUtils.getQuote(annotation);
             }),

             distinctQuotes = distinct(quotes),

             popup = jQuery(
               '<div class="popup">' +
                 '<div class="popup-header">' +
                   '<h3>' + distinctQuotes.join(', ') + '</h3>' +
                 '</div>' +
                 '<div class="snippet">' +
                   '<div class="snippet-body">' +
                     '<div class="previous icon">&#xf104;</div>' +
                     '<div class="snippet-text"></div>' +
                     '<div class="next icon">&#xf105;</div>' +
                   '</div>' +
                   '<div class="snippet-footer">' +
                     '<span class="label"></span>' +
                     '<a class="jump-to-text" href="#" onclick="return false;">JUMP TO TEXT</a>' +
                   '</div>' +
                 '</div>' +
                 '<div><table class="gazetteer-records"></table></div>' +
                '</div>'),

             gazetteerURIs = getDistinctURIs(annotations),

             renderCurrentSnippet = function() {
               API.getAnnotation(annotations[currentSnippet].annotation_id, true)
                  .done(function(annotation) {
                    // TODO attach snippet to template
                    var offset = annotation.context.char_offset,
                        quote = AnnotationUtils.getQuote(annotation),
                        snippet = annotation.context.snippet,
                        formatted =
                          snippet.substring(0, offset) + '<em>' +
                          snippet.substring(offset, offset + quote.length) + '</em>' +
                          snippet.substring(offset + quote.length);

                    popup.find('.snippet-text').html(formatted);
                    popup.find('.label').html('1 OF ' + annotations.length + ' ANNOTATIONS');
                });
             };

         jQuery.each(gazetteerURIs, function(idx, uri) {
           var record = PlaceUtils.getRecord(place, uri),

               recordId = PlaceUtils.parseURI(record.uri),

               element = jQuery(
                 '<tr data-uri="' + record.uri + '">' +
                   '<td class="record-id">' +
                     '<span class="shortcode"></span>' +
                     '<span class="id"></span>' +
                   '</td>' +
                   '<td class="place-details">' +
                     '<h3>' + record.title + '</h3>' +
                     '<p class="description"></p>' +
                     '<p class="date"></p>' +
                   '</td>' +
                 '</tr>');

           if (recordId.shortcode) {
             element.find('.shortcode').html(recordId.shortcode);
             element.find('.id').html(recordId.id);
             element.find('.record-id').css('background-color', recordId.color);
           }

           if (record.descriptions.length > 0)
             element.find('.description').html(record.descriptions[0].description);
           else
             element.find('.description').hide();

           if (record.temporal_bounds)
             element.find('.date').html(
               Formatting.yyyyMMddToYear(record.temporal_bounds.from) + ' - ' +
               Formatting.yyyyMMddToYear(record.temporal_bounds.to));
           else
             element.find('.date').hide();

           popup.find('.gazetteer-records').append(element);
         });

         renderCurrentSnippet();

         popup.on('click', '.previous', function() {
           currentSnippet = Math.max(0, currentSnippet - 1);
           renderCurrentSnippet();
         });

         popup.on('click', '.next', function() {
           currentSnippet = Math.min(annotations.length - 1, currentSnippet + 1);
           renderCurrentSnippet();
         });

         if (annotations.length > 0)
           L.popup().setLatLng(coord).setContent(popup[0]).openOn(map);
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

         computeMarkerScaleFn();

         // After the annotations are loaded, load the places
         return API.listPlacesInDocument(Config.documentId, 0, 2000);
       },

       onPlacesLoaded = function(response) {
         jQuery.each(response.items, function(idx, place) {
           if (place.representative_point) {
             // The epic battle between Leaflet vs. GeoJSON
             var coord = [ place.representative_point[1], place.representative_point[0] ],
                 markerSize = markerScaleFn(getAnnotationsForPlace(place).length);

             L.circleMarker(coord, {
               color       : STROKE_COLOR,
               fillColor   : FILL_COLOR,
               opacity     : 1,
               fillOpacity : 1,
               weight      : 1.5,
               radius: markerSize
             }).addTo(map).on('click', function() {
               renderPopup(coord, place);
             });
           }
         });
       },

       computeMarkerScaleFn = function() {
         var min = 9007199254740991, max = 1,
             k, d;

         // Determine min/max annotations per place
         jQuery.each(annotationsByGazetteerURI, function(uri, annotations) {
           var count = annotations.length;
           if (count < min)
             min = count;
           if (count > max)
             max = count;
         });

         // Marker size y = fn(no_of_annotations) is linear fn according to y = k * x + d
         k = (MAX_MARKER_SIZE - MIN_MARKER_SIZE) / (max - min);
         d = ((MIN_MARKER_SIZE * max) - (MAX_MARKER_SIZE * min)) / (max - min);

         markerScaleFn = function(noOfAnnotations) { return k * noOfAnnotations + d; };
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
