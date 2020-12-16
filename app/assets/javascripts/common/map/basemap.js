define(['common/map/layerswitcher'], function(LayerSwitcher) {

  var DEFAULT_ZOOM = 4,

      DEFAULT_CENTER = new L.LatLng(37.98, 23.73);

  var BaseMap = function(element) {

    var Layers =  {

          /*
          AWMC   : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                     attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                   }),
          */

          OSM    : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                     attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                   }),

          DARE   : L.tileLayer('http:///dh.gu.se/tiles/imperium/{z}/{x}/{y}.png', {
            attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
            minZoom:3,
            maxZoom:11
          }),

          AERIAL : L.tileLayer('http://api.tiles.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGVsYWdpb3MiLCJhIjoiMWRlODMzM2NkZWU3YzkxOGJkMDFiMmFiYjk3NWZkMmUifQ.cyqpSZvhsvBGEBwRfniVrg', {
                     attribution: '<a href="https://www.mapbox.com/about/maps/">&copy; Mapbox</a> <a href="http://www.openstreetmap.org/about/">&copy; OpenStreetMap</a>',
                     maxZoom:22
                   })
        },

        // currentBaseLayer = Layers.AWMC,
        currentBaseLayer = Layers.OSM,

        map = L.map(element[0], {
          center: DEFAULT_CENTER,
          zoom: DEFAULT_ZOOM,
          zoomControl: false,
          layers: [currentBaseLayer]
        }),

        layerSwitcher = new LayerSwitcher(),

        btnLayers = jQuery('.layers'),
        btnZoomIn = jQuery('.zoom-in'),
        btnZoomOut = jQuery('.zoom-out'),

        onChangeLayer = function(name) {
          var layer = Layers[name];
          if (layer && layer !== currentBaseLayer) {
            map.addLayer(layer);
            map.removeLayer(currentBaseLayer);
            currentBaseLayer = layer;
          }
        },

        add = function(addable) {
          addable.addTo(map);
        },

        refresh = function() {
          map.invalidateSize();
        };

    btnLayers.click(function() { layerSwitcher.open(); });
    btnZoomIn.click(function() { map.zoomIn(); });
    btnZoomOut.click(function() { map.zoomOut(); });

    layerSwitcher.on('changeLayer', onChangeLayer);

    this.add = add;
    this.refresh = refresh;
    this.leafletMap = map;
  };

  return BaseMap;

});
