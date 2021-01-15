define([
  'common/config',
  'common/hasEvents',
  'document/annotation/image/iiif/iiifSource'
], function(Config, HasEvents, IIIFSource) {

  var FULLSCREEN_SLIDE_DURATION = 200;

  var Viewer = function(imageProperties) {

    var self = this,

        BASE_URL = jsRoutes.controllers.document.DocumentController
                     .getImageTile(Config.documentId, Config.partSequenceNo, '').absoluteURL(),

        w = imageProperties.width,

        h = imageProperties.height,

        imagePane = jQuery('#image-pane'),

        sidebar = jQuery('.sidebar'),

        iconbar = jQuery('.header-iconbar'),
        infobox = jQuery('.header-infobox'),
        toolbar = jQuery('.header-toolbar'),

        saveMsg = jQuery('.save-msg'),

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
            '<div class="reset-rotation control" title="Reset rotation"><span class="icon">&#xf176;</span></div>' +
            '<div class="fullscreen control icon">&#xf065;</div>' +
          '</div>'),

        resetRotationIcon = controlsEl.find('.reset-rotation span');

    var projection;

        if (Config.contentType === 'IMAGE_UPLOAD') {
          projection = new ol.proj.Projection({
            code: 'ZOOMIFY',
            units: 'pixels',
            extent: [0, -h, w, 0]
          });
        } else if (Config.contentType === 'IMAGE_IIIF') {
          projection = new ol.proj.Projection({
            code: 'IIIF',
            units: 'pixels',
            extent: [0, -h, w, 0]
          });
        } else if (Config.contentType.indexOf('MAP_' === 0)) {
          projection = ol.proj.get('EPSG:3857');
        }

        /* Cf. https://openlayers.org/en/v4.6.5/examples/wmts.html
        resolutions = (function() {
          var size = ol.extent.getWidth(projection.getExtent()) / 256,
              resolutions = new Array(14);

          for (var z = 0; z < 14; ++z) {
            resolutions[z] = size / Math.pow(2, z);
          }
  
          return resolutions;
        })(),

        matrixIds = (function() {
          var matrixIds = new Array(14);

          for (var z = 0; z < 14; ++z) {
            matrixIds[z] = z;
          }
  
          return matrixIds;
        })(),
        */

    var tileSource;
    
        if (Config.contentType === 'IMAGE_UPLOAD') {
          tileSource = new ol.source.Zoomify({
            url: BASE_URL,
            size: [ w, h ]
          });
        } else if (Config.contentType === 'IMAGE_IIIF') {
          tileSource = new IIIFSource(imageProperties);
        } else if (Config.contentType === 'MAP_XYZ') {
          tileSource = new ol.source.XYZ({
            minZoom: 1,
            maxZoom: 14,
            crossOrigin: 'anonymous',
            url: imageProperties.url
          });
        } else if (Config.contentType === 'MAP_WMTS') {
          var projectionExtent = projection.getExtent();
          var size = ol.extent.getWidth(projectionExtent) / 256;
          var resolutions = new Array(14);
          var matrixIds = new Array(14);
          for (var z = 0; z < 14; ++z) {
            // generate resolutions and matrixIds arrays for this WMTS
            resolutions[z] = size / Math.pow(2, z);
            matrixIds[z] = z;
          }

          tileSource = new ol.source.WMTS({
            url: imageProperties.url,
            layer: '0',
            matrixSet: 'EPSG:3857',
            format: 'image/png',
            projection: projection,
            tileGrid: new ol.tilegrid.WMTS({
              origin: ol.extent.getTopLeft(projectionExtent),
              resolutions: resolutions,
              matrixIds: matrixIds,
            }),
            style: 'default',
            wrapX: true,
          })
        }

    var tileLayer = new ol.layer.Tile({
          source: tileSource
        }),

        olMap = new ol.Map({
          target: 'image-pane',
          layers: [ tileLayer ],
          controls: [],
          interactions: ol.interaction.defaults().extend([
            new ol.interaction.DragRotateAndZoom({
              condition: ol.events.condition.altKeyOnly
            })
          ]),
          view: new ol.View({
            projection: projection,
            minResolution: 0.5
          })
        }),

        zoomToExtent = function() {
          olMap.getView().fit(projection.getExtent(), {
            nearest: true,
            constrainResolution: false
          });
        },

        changeZoom = function(increment) {
          var view = olMap.getView(),
              current = view.getZoom();

          view.animate({ zoom: current + increment, duration: 250 });
        },

        updateRotationIcon = function() {
          var rotation = olMap.getView().getRotation();
          resetRotationIcon.css({
            transform: 'rotate(' + rotation + 'rad)',
          });
        },

        resetRotation = function() {
          olMap.getView().animate({ rotation: 0, duration: 250 });
        },

        toggleFullscreen = function() {
          var wasFullscreen = isFullscreen,

              duration = { duration: FULLSCREEN_SLIDE_DURATION },

              toggleHeader = function() {
                var header = iconbar.add(infobox);

                if (wasFullscreen) {
                  header.velocity('slideDown', duration);
                  toolbar.velocity({ 'margin-left': sidebarWidth }, duration);
                } else {
                  header.velocity('slideUp', duration);
                  toolbar.velocity({ 'margin-left': 0 }, duration);
                }
              },

              toggleSidebar = function() {
                var left = (wasFullscreen) ? 0 : - sidebarWidth;
                sidebar.velocity({ left: left }, duration);
              },

              toggleImagePane = function() {
                var top = (wasFullscreen) ? headerHeight + toolbarHeight : toolbarHeight,
                    left = (wasFullscreen) ? sidebarWidth : 0;

                imagePane.animate({
                  top: top,
                  left: left
                }, {
                  duration: FULLSCREEN_SLIDE_DURATION,
                  step: function() { olMap.updateSize(); },
                  complete: function() {
                    olMap.updateSize();
                    self.fireEvent('fullscreen', isFullscreen);
                  }
                });
              };

          // Change state before animation starts
          isFullscreen = !isFullscreen;

          if (isFullscreen) jQuery(document.body).append(saveMsg);
          else iconbar.append(saveMsg);

          saveMsg.toggleClass('fullscreen');

          toggleHeader();
          toggleSidebar();
          toggleImagePane();
        },

        initControls = function() {
          var zoomInBtn = controlsEl.find('.zoom-in'),
              zoomOutBtn = controlsEl.find('.zoom-out'),
              resetRotationBtn = controlsEl.find('.reset-rotation'),
              fullscreenBtn = controlsEl.find('.fullscreen');

          zoomInBtn.click(function() { changeZoom(1); });
          zoomOutBtn.click(function() { changeZoom(-1); });

          olMap.on('postrender', updateRotationIcon);
          resetRotationBtn.click(resetRotation);

          fullscreenBtn.click(toggleFullscreen);

          imagePane.append(controlsEl);
        };

    zoomToExtent();
    initControls();

    this.olMap = olMap;

    HasEvents.apply(this);
  };
  Viewer.prototype = Object.create(HasEvents.prototype);

  return Viewer;

});
