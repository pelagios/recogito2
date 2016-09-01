define(['common/config'], function(Config) {

  var Viewer = function(imageProperties) {

    var BASE_URL = jsRoutes.controllers.document.DocumentController
                     .getImageTile(Config.documentId, Config.partSequenceNo, '').absoluteURL(),

        w = imageProperties.width,

        h = imageProperties.height,

        controlsEl = jQuery(
          '<div class="map-controls">' +
            '<div class="zoom">' +
              '<div class="zoom-in control" title="Zoom in">+</div>' +
              '<div class="zoom-out control" title="Zoom out">&ndash;</div>' +
            '</div>' +
          '</div>'),

        projection = new ol.proj.Projection({
          code: 'ZOOMIFY',
          units: 'pixels',
          extent: [0, 0, w, h]
        }),

        tileSource = new ol.source.Zoomify({
          url: BASE_URL,
          size: [ w, h ]
        }),

        tileLayer = new ol.layer.Tile({ source: tileSource }),

        olMap = new ol.Map({
          target: 'image-pane',
          layers: [ tileLayer ],
          controls: [],
          view: new ol.View({
            projection: projection,
            center: [w / 2, - (h / 2)],
            zoom: 0,
            minResolution: 0.125
          })
        }),

        zoomToExtent = function() {
          olMap.getView().fit([ 0, 0, w, -h ], olMap.getSize());
        },

        changeZoom = function(increment) {
          var view = olMap.getView(),
              currentZoom = view.getZoom(),
              animation = ol.animation.zoom({
                duration: 250, resolution: olMap.getView().getResolution()
              });

          olMap.beforeRender(animation);
          olMap.getView().setZoom(currentZoom + increment);
        },

        initControls = function() {
          controlsEl.find('.zoom-in').click(function() { changeZoom(1); });
          controlsEl.find('.zoom-out').click(function() { changeZoom(-1); });
          jQuery('#image-pane').append(controlsEl);
        };

    zoomToExtent();
    initControls();

    this.olMap = olMap;

  };

  return Viewer;

});
