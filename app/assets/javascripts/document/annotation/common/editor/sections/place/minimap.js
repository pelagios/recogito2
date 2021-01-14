define([], function(Formatting, PlaceUtils) {

  var MiniMap = function(element) {

    var DEFAULT_ZOOM = 4,

        DEFAULT_CENTER = new L.LatLng(37.98, 23.73),

        CENTER_POINT = L.point(82, 70), // TODO derive from element dimensions

        /*
        awmcBase = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),
        */

        osmBase = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
        }),

        map = L.map(element[0], {
          center: DEFAULT_CENTER,
          zoom: DEFAULT_ZOOM,
          zoomControl: false,
          layers: [ osmBase ]
        }),

        markerLayer = L.layerGroup().addTo(map),

        moveTo = function(latLon, opt_zoom) {
          var zoom = (opt_zoom) ? opt_zoom : DEFAULT_ZOOM;

          // Need to set as center first, then we can offset
          window.setTimeout(function() {
            map.invalidateSize();
            map.setView(latLon, zoom, { animate: false });
            map.panTo(map.containerPointToLatLng(CENTER_POINT), { animate: false});
          }, 1); // Make sure this happens after the map was rendered!
        },

        setLocation = function(latLon) {
          if (latLon) {
            markerLayer.clearLayers();
            L.marker(latLon).addTo(markerLayer);
            moveTo(latLon);
            element.removeClass('unlocated');
          } else {
            element.addClass('unlocated');
            moveTo(DEFAULT_CENTER, 1);
          }
        },

        clear = function() {
          element.removeClass('gray');
          markerLayer.clearLayers();
          moveTo(DEFAULT_CENTER);
        };

    this.clear = clear;
    this.setLocation = setLocation;
  };

  return MiniMap;

});
