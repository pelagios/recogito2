define([], function() {

  var PlaceSection = function(parent) {
    var element = (function() {
          var el = jQuery(
            '<div class="section place">' +
              '<div class="map"></div>' +
              '<div class="panel-container">' +
                '<div class="panel"></div>' +
              '</div>' +
            '</div>');

          parent.append(el);
          return el;
        })(),

        awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = new L.Map(element.find('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 3,
          zoomControl: false,
          layers: [ awmc ]
        });
  };

  return PlaceSection;

});
