require([], function() {

  jQuery(document).ready(function() {

    var contentDiv = jQuery('#image-pane'),

        projection = new ol.proj.Projection({
          code: 'ZOOMIFY',
          units: 'pixels',
          extent: [0, 0, 8275, 6514]
        }),

        tileSource = new ol.source.Zoomify({
          url: 'http://localhost:9000/assets/images/41911630-a1ce-4ce1-adf8-4cb961301ec9/',
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
