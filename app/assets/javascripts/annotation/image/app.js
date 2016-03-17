require(['../../common/config'], function(Config) {

  jQuery(document).ready(function() {

    var contentDiv = jQuery('#image-pane'),

        projection = new ol.proj.Projection({
          code: 'ZOOMIFY',
          units: 'pixels',
          extent: [0, 0, 8275, 6514]
        }),

        tileSource = new ol.source.Zoomify({
          url: '/document/' + Config.documentId + '/part/' + Config.partSequenceNo + '/tiles/',
          size: [ 8275, 6514 ]
        }),

        tileLayer = new ol.layer.Tile({ source: tileSource }),

        olMap = new ol.Map({
          target: 'image-pane',
          layers: [ tileLayer ],
          view: new ol.View({
            projection: projection,
            center: [4137, -3257],
            zoom: 0,
            minResolution: 0.125
          })
        });

  });

});
