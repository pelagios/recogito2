define(['document/map/palette'], function(Palette) {

  var BASE_STYLE = {
        color       : Palette.DEFAULT_STROKE_COLOR,
        fillColor   : Palette.DEFAULT_FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 0.5
      };

  var ShapeLayer = function(map) {

    var shapeLayer = L.layerGroup(),

        addShape = function(place) {
          return L.geoJson(place.representative_geometry, BASE_STYLE).addTo(shapeLayer);
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
