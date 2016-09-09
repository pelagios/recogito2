define([
  'common/config',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Config, Layer, Style) {

  var RectDrawingTool = function(olMap) {

    var isEnabled = false, // Keep track, so we don't add the draw interaction multiple times

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
          };

          return new ol.interaction.Draw({
            source: rectVectorSource,
            type: 'LineString',
            geometryFunction: geometryFunction,
            maxPoints: 2
          });
        })(),

        setEnabled = function(enabled) {
          if (enabled && !isEnabled)
            olMap.addInteraction(drawInteraction);
          else if (!enabled && isEnabled)
            olMap.removeInteraction(drawInteraction);

          isEnabled = enabled;
        },

        clearSelection = function() {

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
