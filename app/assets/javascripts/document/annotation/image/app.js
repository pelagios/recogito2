require(['../../../common/config'], function(Config) {

  jQuery(document).ready(function() {

    var contentDiv = jQuery('#image-pane'),

        BASE_URL = '/document/' + Config.documentId + '/part/' + Config.partSequenceNo + '/tiles/',

        initDropdown = function(element) {
          element.hide();
          element.parent().hover(
            function() { element.show(); },
            function() { element.hide(); });
        },

        initImage = function(width, height) {
          var projection = new ol.proj.Projection({
                code: 'ZOOMIFY',
                units: 'pixels',
                extent: [0, 0, width, height]
              }),

              tileSource = new ol.source.Zoomify({
                url: BASE_URL,
                size: [ width, height ]
              }),

              tileLayer = new ol.layer.Tile({ source: tileSource }),

              olMap = new ol.Map({
                target: 'image-pane',
                layers: [ tileLayer ],
                view: new ol.View({
                  projection: projection,
                  center: [width / 2, - (height / 2)],
                  zoom: 0,
                  minResolution: 0.125
                })
              });
        },

        loadManifest = function() {
          jQuery.ajax({
            type: 'GET',
            url: BASE_URL + 'ImageProperties.xml',
            success: function(response) {
              // jQuery handles XML parsing for us automagically
              var props = jQuery(response).find('IMAGE_PROPERTIES'),
                  width = parseInt(props.attr('WIDTH')),
                  height = parseInt(props.attr('HEIGHT'));

              initImage(width, height);
            },
            error: function(error) {
              console.log(error);
            }
          });
        };

    initDropdown(jQuery('.dropdown'));
    loadManifest();
  });

});
