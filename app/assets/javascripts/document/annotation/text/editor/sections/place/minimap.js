define([], function(Formatting, PlaceUtils) {

  var MiniMap = function(element) {

    var DEFAULT_ZOOM = 4,

        DEFAULT_CENTER = new L.LatLng(37.98, 23.73),

        CENTER_POINT = L.point(82, 70), // TODO derive from element dimensions

        awmcBase = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(element[0], {
          center: DEFAULT_CENTER,
          zoom: DEFAULT_ZOOM,
          zoomControl: false,
          layers: [ awmcBase ]
        }),

        markerLayer = L.layerGroup().addTo(map),

        moveTo = function(latLon) {
          // Need to set as center first, then we can offset
          window.setTimeout(function() {
            map.invalidateSize();
            map.setView(latLon, DEFAULT_ZOOM, { animate: false });
            map.panTo(map.containerPointToLatLng(CENTER_POINT), { animate: false});
          }, 1); // Make sure this happens after the map was rendered!
        },

        setLocation = function(latLon) {
          markerLayer.clearLayers();
          L.marker(latLon).addTo(markerLayer);
          moveTo(latLon);
        },

        clear = function() {
          markerLayer.clearLayers();
          moveTo(DEFAULT_CENTER);
        };

    this.clear = clear;
    this.setLocation = setLocation;
  };

  return MiniMap;

});
