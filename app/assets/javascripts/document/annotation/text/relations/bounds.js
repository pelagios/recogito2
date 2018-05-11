define([], function() {

  return {

    getUnionBounds : function(elements) {
      var bounds = elements.toArray().map(function(el) {
            return el.getBoundingClientRect();
          }),

          top = bounds.map(function(b) { return b.top; }),
          right = bounds.map(function(b) { return b.right; }),
          bottom = bounds.map(function(b) { return b.bottom; }),
          left = bounds.map(function(b) { return b.left; }),

          minTop = Math.min.apply(null, top),
          maxRight = Math.max.apply(null, right),
          maxBottom = Math.min.apply(null, bottom),
          minLeft = Math.min.apply(null, left);

      return {
        top   : minTop,
        right : maxRight,
        bottom: maxBottom,
        left  : minLeft,
        width : maxRight - minLeft,
        height: maxBottom - minTop
      };
    },

    // Helper to convert client (viewport) bounds to offset bounds
    toOffsetBounds : function(clientBounds, offsetContainer) {
      var offset = offsetContainer.offset(),
          left = Math.round(clientBounds.left - offset.left),
          top = Math.round(clientBounds.top - offset.top + jQuery(window).scrollTop());

      return {
        left  : left,
        top   : top,
        right : Math.round(left + clientBounds.width),
        bottom: Math.round(top + clientBounds.height),
        width : Math.round(clientBounds.width),
        height: Math.round(clientBounds.height)
      };
    },

    // Shorthand for getting top handle position on bounds
    getTopHandleXY : function(bounds) {
      return [
        bounds.left + bounds.width / 2 + 0.5,
        bounds.top
      ];
    },

    // Shorthand for getting bottom handle position on bounds
    getBottomHandleXY : function(bounds) {
      return [
        bounds.left + bounds.width / 2 - 0.5,
        bounds.bottom
      ];
    }

  };

});
