define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'],

  function(Config, AbstractSelectionHandler) {

  var CLICK_DURATION_THRESHOLD_MS = 100,

      POINT_LAYER_STYLE = new ol.style.Style({
        image: new ol.style.Circle({
          radius : 8,
          fill   : new ol.style.Fill({ color: '#ff0000' }),
          stroke : new ol.style.Stroke({ color: '#bada55', width: 1 })
        })
      });

  var PointSelectionHandler = function(containerEl, olMap) {

    var self = this,

        currentSelection = false,

        pointVectorSource = new ol.source.Vector({}),

        drawPoint = function(xy) {
          var pointFeature = new ol.Feature({
            'geometry': new ol.geom.Point(xy)
          });

          // pointVectorSource.addFeature(pointFeature);
        },

        onClick = function(e) {
          var offset = jQuery(containerEl).offset(),

              annotation = {
                annotates: {
                  document_id: Config.documentId,
                  filepart_id: Config.partId,
                  content_type: Config.contentType
                },
                // TODO anchor
                bodies: []
              },

              xy = olMap.getPixelFromCoordinate(e.coordinate),

              bounds = {
                top    : xy[1] + offset.top,
                right  : xy[0] + offset.left,
                bottom : xy[1] + offset.top,
                left   : xy[0] + offset.left,
                width  : 0,
                height : 0
              };

          // TODO dummy, so we can see something
          drawPoint(e.coordinate);

          currentSelection = { annotation: annotation, bounds: bounds };
          self.fireEvent('select', currentSelection);
        },

        getSelection = function() {
          return currentSelection;
        },

        clearSelection = function() {

        };

    olMap.addLayer(new ol.layer.Vector({
      source: pointVectorSource,
      style: POINT_LAYER_STYLE
    }));

    olMap.on('click', onClick);

    this.getSelection = getSelection;
    this.clearSelection = clearSelection;

    AbstractSelectionHandler.apply(this);
  };
  PointSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

  return PointSelectionHandler;

});
