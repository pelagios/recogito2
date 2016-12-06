define(['common/hasEvents'], function(HasEvents) {

  var Layer = function(olMap) {
    this.olMap = olMap;
    HasEvents.apply(this);
  };
  Layer.prototype = Object.create(HasEvents.prototype);

  /** Utility to compute pixel distance between a screen location and a map coordinate **/
  Layer.prototype.computePxDistance = function(px, coord) {
    var otherPx = this.olMap.getPixelFromCoordinate(coord),
        dx = px[0] - otherPx[0],
        dy = px[1] - otherPx[1];

    return Math.sqrt(dx * dx + dy * dy);
  };

  /** Utility to convert a point to (point-sized) bounds **/
  Layer.prototype.pointArrayToBounds = function(coordinates) {
    return coordinates.map(function(coord) {
      return { x: coord[0], y: - coord[1] };
    });
  };

  /**
   * TODO make this more performant (indexing? tricky though, as ID is provided async...)
   */
  Layer.prototype.findFeatureByAnnotationId = function(id, vectorSource) {
    var feature;

    vectorSource.forEachFeature(function(f) {
      var a = f.get('annotation');
      if (a.annotation_id === id) {
        feature = f;
        return true; // Breaks from the loop
      }
    });

    return feature;
  };

  return Layer;

});
