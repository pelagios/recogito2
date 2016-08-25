/** 2D vector computation utilities **/
define([], function() {

  return {

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
    },

    /** Computes the angle between two vectors **/
    angleBetween : function(a, b) {
      var dotProduct = a[0] * b[0] + a[1] * b[1];
      return Math.acos(dotProduct);
    }

  };

});
