define([], function() {

  return {

    SVG_NAMESPACE : "http://www.w3.org/2000/svg",

    // Rounded corner arc radius
    BORDER_RADIUS : 3,

    // Horizontal distance between connection line and annotation highlight
    LINE_DISTANCE : 6,

    // Possible rounded corner SVG arc configurations: clock position + clockwise/counterclockwise
    ARC_0CW : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 1 ' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS,
    ARC_0CC : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 0 -' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS,
    ARC_3CW : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 1 -' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS,
    ARC_3CC : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 0 -' + this.BORDER_RADIUS + ',-' + this.BORDER_RADIUS,
    ARC_6CW : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 1 -' + this.BORDER_RADIUS + ',-' + this.BORDER_RADIUS,
    ARC_6CC : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 0 ' + this.BORDER_RADIUS + ',-' + this.BORDER_RADIUS,
    ARC_9CW : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 1 ' + this.BORDER_RADIUS + ',-' + this.BORDER_RADIUS,
    ARC_9CC : 'a' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS + ' 0 0 0 ' + this.BORDER_RADIUS + ',' + this.BORDER_RADIUS,

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
        bounds.left + bounds.width / 2 - 0.5,
        bounds.top - 0.5
      ];
    },

    // Shorthand for getting bottom handle position on bounds
    getBottomHandleXY : function(bounds) {
      return [
        bounds.left + bounds.width / 2 - 0.5,
        bounds.bottom + 0.5
      ];
    }

  };

});
