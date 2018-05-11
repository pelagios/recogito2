define([], function() {

  var BORDER_RADIUS = 3;

  return {

    SVG_NAMESPACE : "http://www.w3.org/2000/svg",

    // Rounded corner arc radius
    BORDER_RADIUS : BORDER_RADIUS,

    // Horizontal distance between connection line and annotation highlight
    LINE_DISTANCE : 6.5,

    // Possible rounded corner SVG arc configurations: clock position + clockwise/counterclockwise
    ARC_0CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,
    ARC_0CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
    ARC_3CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
    ARC_3CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
    ARC_6CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
    ARC_6CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
    ARC_9CW : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
    ARC_9CC : 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,

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
