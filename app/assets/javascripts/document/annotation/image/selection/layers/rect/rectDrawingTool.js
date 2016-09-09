define([
  'common/config',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Config, Layer, Style) {

  var RectDrawingTool = function(olMap) {

    var self = this,

        isEnabled = false, // Keep track, so we don't add the draw interaction multiple times

        rectVectorSource = new ol.source.Vector({}),

        drawInteraction = (function() {
          var geometryFunction = function(coordinates, g) {
                var geometry = g || new ol.geom.Polygon(),
                    start = coordinates[0],
                    end = coordinates[1];

                geometry.setCoordinates([
                  [start, [start[0], end[1]], end, [end[0], start[1]], start]
                ]);

                return geometry;
              },

              interaction = new ol.interaction.Draw({
                source: rectVectorSource,
                type: 'LineString',
                geometryFunction: geometryFunction,
                maxPoints: 2
              }),

              onDrawEnd = function(e) {
                var coords = (e.feature.getGeometry().getCoordinates())[0],

                    x = Math.round(coords[0][0]),
                    y = - Math.round(coords[0][1]),
                    w = Math.round(coords[2][0] - x),
                    h = - Math.round(coords[1][1] + y),

                    anchor = 'rect:x=' + x + ',y=' + y + ',w=' + w + ',h=' + h,

                    annotation = {
                      annotates: {
                        document_id: Config.documentId,
                        filepart_id: Config.partId,
                        content_type: Config.contentType
                      },
                      anchor: anchor,
                      bodies: []
                    },

                    mapBounds = {
                      top    : coords[0][1],
                      right  : x + w,
                      bottom : coords[1][1],
                      left   : x,
                      width  : w,
                      height : h
                    };

                self.fireEvent('newSelection', {
                  isNew: true,
                  annotation: annotation,
                  mapBounds: mapBounds
                });
              };

          interaction.on('drawend', onDrawEnd);

          return interaction;
        })(),

        setEnabled = function(enabled) {
          if (enabled && !isEnabled)
            olMap.addInteraction(drawInteraction);
          else if (!enabled && isEnabled)
            olMap.removeInteraction(drawInteraction);

          isEnabled = enabled;
        },

        clearSelection = function() {
          rectVectorSource.clear(true);
        };

    olMap.addLayer(new ol.layer.Vector({
      source: rectVectorSource
      // style: Style.POINT_HI
    }));

    this.setEnabled = setEnabled;
    this.clearSelection = clearSelection;
    this.createNewSelection = function() {}; // Not needed
    this.updateSize = function() {}; // Not needed

    Layer.apply(this, [ olMap ]);
  };
  RectDrawingTool.prototype = Object.create(Layer.prototype);

  return RectDrawingTool;

});
