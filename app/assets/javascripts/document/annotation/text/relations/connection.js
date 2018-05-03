/** The relationship connection path **/
define([], function() {

  var SVG_NS = "http://www.w3.org/2000/svg",

      // Rounded corner arc radius
      BORDER_RADIUS = 3,

      // Horizontal distance between connection line and annotation highlight
      LINE_DISTANCE = 6,

      // Possible rounded corner SVG arc configurations: clock position + clockwise/counterclockwise
      ARC_0CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_0CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_3CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_3CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_6CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_6CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_9CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_9CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,

      // Helper to convert client (viewport) bounds to offset bounds
      toOffsetBounds = function(clientBounds, offsetContainer) {
        var offset = offsetContainer.offset(),
            left = Math.round(clientBounds.left - offset.left),
            top = Math.round(clientBounds.top - offset.top + jQuery(window).scrollTop());

        return {
          left  : left,
          top   : top,
          right : left + clientBounds.width,
          bottom: top + clientBounds.height,
          width : clientBounds.width,
          height: clientBounds.height
        };
      },

      // Shorthand for getting top handle position on bounds
      getTopHandleXY = function(bounds) {
        return [
          bounds.left + bounds.width / 2 - 0.5,
          bounds.top - 0.5
        ];
      },

      // Shorthand for getting bottom handle position on bounds
      getBottomHandleXY = function(bounds) {
        return [
          bounds.left + bounds.width / 2 - 0.5,
          bounds.bottom + 0.5
        ];
      };

  var Connection = function(svgEl, fromSelection, opt_toSelection) {

        // { annotation: ..., bounds: ... }
    var toSelection = opt_toSelection,

        svg = jQuery(svgEl), // shorthand

        // Note that the selection bounds are useless after scrolling or resize. They
        // represent viewport bounds at the time of selection, so we store document
        // offsets instead
        fromAnnotation = fromSelection.annotation,
        fromBounds = toOffsetBounds(fromSelection.bounds, svg),

        toAnnotation, toBounds,

        // SVG elements
        path = document.createElementNS(SVG_NS, 'path'),
        startHandle = document.createElementNS(SVG_NS, 'circle'),
        endHandle = document.createElementNS(SVG_NS, 'circle'),

        setEnd = function(xyOrElement) {
          if (xyOrElement instanceof Array) {
            return xyOrElement; // Do nothing
          } else {
            // toAnnotation = xyOrSelection.annotation;
            // toBounds = toOffsetBounds(xyOrSelection.bounds, svg);
            toBounds = toOffsetBounds(xyOrElement.getBoundingClientRect(), svg);
            return (fromBounds.top > toBounds.top) ? getBottomHandleXY(toBounds) : getTopHandleXY(toBounds);
         }
        },

        update = function(endXYorSelection) {
          var end = setEnd(endXYorSelection),

              startsAtTop = end[1] <= (fromBounds.top + fromBounds.height / 2),

              start = (startsAtTop) ? getTopHandleXY(fromBounds) : getBottomHandleXY(fromBounds),

              deltaX = end[0] - start[0],
              deltaY = end[1] - start[1],

              d = LINE_DISTANCE - BORDER_RADIUS, // Shorthand: vertical straight line length

              // Path that starts at the top edge of the annotation highlight
              compileBottomPath = function() {
                var arc1 = (deltaX > 0) ? ARC_9CC : ARC_3CW,
                    arc2 = (deltaX > 0) ? ARC_0CW : ARC_0CC;

                return 'M' + start[0] +
                       ' ' + start[1] +
                       'v' + d +
                       arc1 +
                       'h' + (deltaX - 2 * Math.sign(deltaX) * BORDER_RADIUS) +
                       arc2 +
                       'V' + end[1];
              },

              // Path that starts at the bottom edge of the annotation highlight
              compileTopPath = function() {
                var arc1 = (deltaX > 0) ? ARC_9CW : ARC_3CC,
                    arc2 = (deltaX > 0) ?
                      (deltaY >= 0) ? ARC_0CW : ARC_6CC :
                      (deltaY >= 0) ? ARC_0CC : ARC_6CW;

                return 'M' + start[0] +
                       ' ' + start[1] +
                       'v-' + (LINE_DISTANCE - BORDER_RADIUS) +
                       arc1 +
                       'h' + (deltaX - 2 * Math.sign(deltaX) * BORDER_RADIUS) +
                       arc2 +
                       'V' + end[1];
              };

          startHandle.setAttribute('cx', start[0]);
          startHandle.setAttribute('cy', start[1]);
          startHandle.setAttribute('r', 4);
          startHandle.setAttribute('class', 'start');

          endHandle.setAttribute('cx', end[0]);
          endHandle.setAttribute('cy', end[1]);
          endHandle.setAttribute('r', 3.5);
          endHandle.setAttribute('class', 'end');

          if (startsAtTop) path.setAttribute('d', compileTopPath());
          else path.setAttribute('d', compileBottomPath());
        },

        destroy = function() {

        };

    svgEl.appendChild(path);
    svgEl.appendChild(startHandle);
    svgEl.appendChild(endHandle);

    this.update = update;
    this.destroy = destroy;
  };

  return Connection;

});
