define([
  'common/config',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Config, Layer, Style) {

  var PointDrawingTool = function(olMap) {

    var self = this,

        pointVectorSource = new ol.source.Vector({}),

        drawPoint = function(coordinate) {
          var pointFeature = new ol.Feature({
                'geometry': new ol.geom.Point(coordinate)
              });

          pointVectorSource.clear(true);
          pointVectorSource.addFeature(pointFeature);
        },

        /** The simplest possible drawing case: draws a point at the given coordinate **/
        createNewSelection = function(e) {
          var x = Math.round(e.coordinate[0]),
              y = Math.abs(Math.round(e.coordinate[1])),

              annotation = {
                annotates: {
                  document_id: Config.documentId,
                  filepart_id: Config.partId,
                  content_type: Config.contentType
                },
                anchor: 'point:' + x + ',' + y,
                bodies: []
              },

              mapBounds = this.pointToBounds(e.coordinate);

          drawPoint(e.coordinate);

          self.fireEvent('newSelection', {
            isNew: true,
            annotation: annotation,
            mapBounds: mapBounds
          });
        },

        clearSelection = function() {

        },

        setEnabled = function(enabled) {
          // Since the point drawing tool uses OL's native features,
          // nothing is needed to specifcally enable the tool
        };

    olMap.addLayer(new ol.layer.Vector({
      source: pointVectorSource,
      style: Style.POINT_HI
    }));

    this.createNewSelection = createNewSelection;
    this.clearSelection = clearSelection;
    this.setEnabled = setEnabled;

    Layer.apply(this, [ olMap ]);
  };
  PointDrawingTool.prototype = Object.create(Layer.prototype);

  return PointDrawingTool;

});
