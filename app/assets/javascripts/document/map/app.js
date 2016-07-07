require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config',
  'document/map/layerswitcher',
  'document/map/mapPopup'], function(Formatting, AnnotationUtils, PlaceUtils, API, Config, LayerSwitcher, MapPopup) {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4,

      FILL_COLOR = '#31a354',

      STROKE_COLOR = '#006d2c',

      TOUCH_DISTANCE_THRESHOLD = 18;

  jQuery(document).ready(function() {
    var Layers =  {

          DARE   : L.tileLayer('http://pelagios.org/tilesets/imperium/{z}/{x}/{y}.png', {
                     attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                     minZoom:3,
                     maxZoom:11
                   }),

          AWMC   : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                     attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                   }),

          OSM    : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                     attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                   }),

          AERIAL : L.tileLayer('http://api.tiles.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGVsYWdpb3MiLCJhIjoiMWRlODMzM2NkZWU3YzkxOGJkMDFiMmFiYjk3NWZkMmUifQ.cyqpSZvhsvBGEBwRfniVrg', {
                     attribution: '<a href="https://www.mapbox.com/about/maps/">&copy; Mapbox</a> <a href="http://www.openstreetmap.org/about/">&copy; OpenStreetMap</a>',
                     maxZoom:22
                   })

        },

        currentBaseLayer = Layers.AWMC,

        map = L.map(jQuery('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 4,
          zoomControl: false,
          layers: [ currentBaseLayer ]
       }),

       markerLayer = L.layerGroup().addTo(map),

       layerSwitcher = new LayerSwitcher(),

       btnLayers = jQuery('.layers'),

       /** Lookup table { gazetteerUri -> [ annotation] } **/
       annotationsByGazetteerURI = {},

       markerScaleFn,

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
             var latlng = [ place.representative_point[1], place.representative_point[0] ],
                 markerSize = markerScaleFn(getAnnotationsForPlace(place).length),
                 marker = L.circleMarker(latlng, {
                   color       : STROKE_COLOR,
                   fillColor   : FILL_COLOR,
                   opacity     : 1,
                   fillOpacity : 1,
                   weight      : 1.5,
                   radius: markerSize
                 }).addTo(markerLayer);

             marker.place = place; // TODO Hack! Clean this up

             marker.on('click', function() {
               var popup = new MapPopup(latlng, place, getAnnotationsForPlace(place));
               popup.addTo(map);
             });
           }
         });
       },

       onChangeLayer = function(name) {
         var layer = Layers[name];
         if (layer && layer !== currentBaseLayer) {
           map.addLayer(layer);
           map.removeLayer(currentBaseLayer);
           currentBaseLayer = layer;
         }
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
       },

       /**
        * Selects the marker nearest the given latlng. This is primarily a
        * means to support touch devices, where touch events will usually miss
        * the markers because they are too small for properly hitting them.
        *
        * TODO this could be heavily optimized by some sort of spatial indexing or bucketing,
        * but seems to work reasonably well even for lots of markers.
        *
        */
       selectNearest = function(latlng, maxDistance) {
         var xy = map.latLngToContainerPoint(latlng),
             nearest = { distSq: 9007199254740992 }, // Distance to nearest initialied with Integer.MAX
             nearestXY, distPx;

         jQuery.each(markerLayer.getLayers(), function(idx, marker) {
           var markerLatLng = marker.getBounds().getCenter(),
               distSq =
                 Math.pow(latlng.lat - markerLatLng.lat, 2) +
                 Math.pow(latlng.lng - markerLatLng.lng, 2);

           if (distSq < nearest.distSq)
             nearest = { marker: marker, latlng: markerLatLng, distSq: distSq };
         });

         if (nearest.marker) {
           nearestXY = map.latLngToContainerPoint(nearest.latlng);
           distPx =
             Math.sqrt(
               Math.pow((xy.x - nearestXY.x), 2) +
               Math.pow((xy.y - nearestXY.y), 2));

           if (distPx < maxDistance)
             renderPopup(nearest.latlng, nearest.marker.place);
         }
       };

    btnLayers.click(function() { layerSwitcher.open(); });

    map.on('click', function(e) {
      selectNearest(e.latlng, TOUCH_DISTANCE_THRESHOLD);
    });

    layerSwitcher.on('changeLayer', onChangeLayer);

    API.listAnnotationsInDocument(Config.documentId)
       .then(onAnnotationsLoaded)
       .done(onPlacesLoaded)
       .fail(onLoadError);
  });

});
