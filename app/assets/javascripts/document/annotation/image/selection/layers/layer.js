define(['common/hasEvents'], function(HasEvents) {

  var Layer = function(olMap) {
    this.olMap = olMap;

    // Default color
    this.color = {
      r: 50,
      g: 50,
      b: 50,
      a: 0.2,
      hex: '#323232'
    };

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

  Layer.prototype.setColor = function(color) {
    this.color = color;
    this.redraw();
  };

  Layer.prototype.getColorRGB = function() {
    return 'rgb(' + this.color.r + ',' + this.color.g + ',' + this.color.b + ')';
  };

  Layer.prototype.getColorRGBA = function() {
    return 'rgba(' + this.color.r + ',' + this.color.g + ',' + this.color.b + ',' + this.color.a + ')';
  };

  Layer.prototype.getColorHex = function() {
    return this.color.hex;
  };

  Layer.prototype.getStrokeOpacity = function() {
    return Math.min(1, this.color.a + 0.3);
  };

  Layer.prototype.getFillOpacity = function() {
    return this.color.a;
  };

  return Layer;

});
