define([], function() {

  var DEFAULT_ZOOM = 4,

      DEFAULT_CENTER = new L.LatLng(37.98, 23.73);

  var Map = function(element) {

    var awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(element[0], {
          center: DEFAULT_CENTER,
          zoom: DEFAULT_ZOOM,
          zoomControl: false,
          layers: [ awmc ]
        }),

        markerLayer = L.layerGroup().addTo(map),

        addMarker = function(latLon) {
          return L.marker(latLon).addTo(markerLayer);
        },

        clear = function() {
          markerLayer.clearLayers();
        },

        refresh = function() {
          map.invalidateSize();
        };

    this.addMarker = addMarker;
    this.clear = clear;
    this.refresh = refresh;
  };

  return Map;

});
