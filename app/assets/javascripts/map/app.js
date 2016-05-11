require(['../common/apiConnector', '../common/config'], function(API, Config) {

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

       onAnnotationsLoaded = function(response) {
         console.log(response);
       },

       onAnnotationsLoadError = function(error) {
         // TODO implement
       };

    API.loadAnnotations(Config.documentId)
       .done(onAnnotationsLoaded)
       .fail(onAnnotationsLoadError);
  });

});
