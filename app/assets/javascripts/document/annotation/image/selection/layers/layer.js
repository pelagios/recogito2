define([], function() {

  var Layer = function(containerEl, olMap) {
    this.containerEl = containerEl;
    this.olMap = olMap;
  };

  /** Utility to compute pixel distance between a screen location and a map coordinate **/
  Layer.prototype.computePxDistance = function(px, coord) {
    var otherPx = this.olMap.getPixelFromCoordinate(coord),
        dx = px[0] - otherPx[0],
        dy = px[1] - otherPx[1];

    return Math.sqrt(dx * dx + dy * dy);
  };

  /** Utility to convert a point to (point-sized) bounds **/
  Layer.prototype.pointToBounds = function(coordinate) {
    return {
      top    : coordinate[1],
      right  : coordinate[0],
      bottom : coordinate[1],
      left   : coordinate[0],
      width  : 0,
      height : 0
    };
  };

  return Layer;

});
