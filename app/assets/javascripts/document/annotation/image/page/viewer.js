define(['common/config'], function(Config) {

  var FULLSCREEN_SLIDE_DURATION = 200;

  var Viewer = function(imageProperties) {

    var BASE_URL = jsRoutes.controllers.document.DocumentController
                     .getImageTile(Config.documentId, Config.partSequenceNo, '').absoluteURL(),

        w = imageProperties.width,

        h = imageProperties.height,

        imagePane = jQuery('#image-pane'),

        sidebar = jQuery('.sidebar'),

        iconbar = jQuery('.header-iconbar'),
        infobox = jQuery('.header-infobox'),
        toolbar = jQuery('.header-toolbar'),

        /** Heights/widths we need for fullscreen toggle animation **/
        headerHeight = iconbar.outerHeight() + infobox.outerHeight(),
        toolbarHeight = toolbar.outerHeight(),
        sidebarWidth = sidebar.outerWidth(),

        isFullscreen = false,

        controlsEl = jQuery(
          '<div class="map-controls">' +
            '<div class="zoom">' +
              '<div class="zoom-in control" title="Zoom in">+</div>' +
              '<div class="zoom-out control" title="Zoom out">&ndash;</div>' +
            '</div>' +
            '<div class="fullscreen control icon">&#xf065;</div>' +
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

        toggleFullscreen = function() {
          var wasFullscreen = isFullscreen,

              toggleHeader = function() {
                var header = iconbar.add(infobox);
                if (wasFullscreen) {
                  header.slideDown(FULLSCREEN_SLIDE_DURATION);
                  toolbar.animate({ 'margin-left': sidebarWidth }, FULLSCREEN_SLIDE_DURATION);
                } else {
                  header.slideUp(FULLSCREEN_SLIDE_DURATION);
                  toolbar.animate({ 'margin-left': 0 }, FULLSCREEN_SLIDE_DURATION);
                }
              },

              toggleSidebar = function() {
                var left = (wasFullscreen) ? 0 : - sidebarWidth;
                sidebar.animate({ left: left }, FULLSCREEN_SLIDE_DURATION);
              },

              toggleImagePane = function() {
                var top = (wasFullscreen) ? headerHeight + toolbarHeight : toolbarHeight,
                    left = (wasFullscreen) ? sidebarWidth : 0;

                imagePane.animate({
                  top: top,
                  left: left
                }, {
                  duration: FULLSCREEN_SLIDE_DURATION,
                  step: function() { olMap.updateSize(); }
                });
              };

          // Change state before animation starts
          isFullscreen = !isFullscreen;

          toggleHeader();
          toggleSidebar();
          toggleImagePane();
        },

        initControls = function() {
          var zoomIn = controlsEl.find('.zoom-in'),
              zoomOut = controlsEl.find('.zoom-out'),
              fullscreen = controlsEl.find('.fullscreen');

          zoomIn.click(function() { changeZoom(1); });
          zoomIn.click(function() { changeZoom(-1); });

          fullscreen.click(toggleFullscreen);

          imagePane.append(controlsEl);
        };

    zoomToExtent();
    initControls();

    this.olMap = olMap;

  };

  return Viewer;

});
