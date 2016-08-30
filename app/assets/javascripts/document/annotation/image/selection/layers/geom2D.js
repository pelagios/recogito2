/** 2D vector computation utilities **/
define([], function() {

  return {

    /** Computes the angle between two vectors **/
    angleBetween : function(a, b) {
      var dotProduct = a[0] * b[0] + a[1] * b[1];
      return Math.acos(dotProduct);
    },

    /** Tests if the given coordinate intersects the rectangle **/
    intersects : function(x, y, rect) {
      var inside = false,
          j = rect.length - 1,
          i;

      for (i=0; i<4; i++) {
        if ((rect[i].y > y) != (rect[j].y > y) &&
            (x < (rect[j].x - rect[i].x) * (y - rect[i].y) / (rect[j].y-rect[i].y) + rect[i].x)) {
              inside = !inside;
        }
        j = i;
      }

      return inside;
    },

    /** Computes the length of a vector **/
    len : function(x1, y1, x2, y2) {
      if (y1)
        // Treat input as a tuple of coordinates
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
      else
        // Treat input as a vector
        return Math.sqrt(x1[0] * x1[0] + x1[1] * x1[1]);
    },

    /** Normalizes a vector to length = 1 **/
    normalize : function(vector) {
      var l = this.len(vector);
      return [ vector[0] / l, vector[1] / l ];
    }

  };

});
