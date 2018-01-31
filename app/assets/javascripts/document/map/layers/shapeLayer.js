define([], function() {

  var ShapeLayer = function(map, mapStyle) {

    var shapeLayer = L.layerGroup(),

        addShape = function(place) {
          var style = mapStyle.getShapeStyle(place, map.getAnnotationsForPlace(place));
          return L.geoJson(place.representative_geometry, style).addTo(shapeLayer);
        },

        // TODO clean up - shouldn't expose this to the outside
        // TODO currently used by map.selectNearest
        getLayers = function() {
          return shapeLayer.getLayers();
        };

    map.add(shapeLayer);

    this.addShape = addShape;
    this.getLayers = getLayers;
    this.init = function() {}; // For future use
  };

  return ShapeLayer;

});
