define(['document/annotation/text/relations/shapes'], function(Shapes) {

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
        path = document.createElementNS(Shapes.SVG_NAMESPACE, 'path'),
        startHandle = document.createElementNS(Shapes.SVG_NAMESPACE, 'circle'),
        endHandle = document.createElementNS(Shapes.SVG_NAMESPACE, 'circle'),

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

              d = Shapes.LINE_DISTANCE - Shapes.BORDER_RADIUS, // Shorthand: vertical straight line length

              // Path that starts at the top edge of the annotation highlight
              compileBottomPath = function() {
                var arc1 = (deltaX > 0) ? Shapes.ARC_9CC : Shapes.ARC_3CW,
                    arc2 = (deltaX > 0) ? Shapes.ARC_0CW : Shapes.ARC_0CC;

                return 'M' + start[0] +
                       ' ' + start[1] +
                       'v' + d +
                       arc1 +
                       'h' + (deltaX - 2 * Math.sign(deltaX) * Shapes.BORDER_RADIUS) +
                       arc2 +
                       'V' + end[1];
              },

              // Path that starts at the bottom edge of the annotation highlight
              compileTopPath = function() {
                var arc1 = (deltaX > 0) ? Shapes.ARC_9CW : Shapes.ARC_3CC,
                    arc2 = (deltaX > 0) ?
                      (deltaY >= 0) ? Shapes.ARC_0CW : Shapes.ARC_6CC :
                      (deltaY >= 0) ? Shapes.ARC_0CC : Shapes.ARC_6CW;

                return 'M' + start[0] +
                       ' ' + start[1] +
                       'v-' + (Shapes.LINE_DISTANCE - Shapes.BORDER_RADIUS) +
                       arc1 +
                       'h' + (deltaX - 2 * Math.sign(deltaX) * Shapes.BORDER_RADIUS) +
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
