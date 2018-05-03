define(['document/annotation/text/relations/shapes'], function(Shapes) {

  var Connection = function(svgEl, fromNode, opt_toNode) {

    var svg = jQuery(svgEl), // shorthand

        // { annotation: ..., elements: ... }
        toNode = opt_toNode, toBounds,

        // Note that the selection bounds are useless after scrolling or resize. They
        // represent viewport bounds at the time of selection, so we store document
        // offsets instead
        fromBounds = Shapes.toOffsetBounds(fromNode.elements[0].getBoundingClientRect(), svg),

        // SVG elements
        path = document.createElementNS(Shapes.SVG_NAMESPACE, 'path'),
        startHandle = document.createElementNS(Shapes.SVG_NAMESPACE, 'circle'),
        endHandle = document.createElementNS(Shapes.SVG_NAMESPACE, 'circle'),

        // [x,y] array or node object
        currentEnd,

        dragTo = function(xyOrNode) {
          currentEnd = xyOrNode;

          if (xyOrNode.elements) {
            toNode = xyOrNode;
            toBounds = Shapes.toOffsetBounds(xyOrNode.elements[0].getBoundingClientRect(), svg);
          }
        },

        getEnd = function() {
          if (currentEnd instanceof Array)
            return currentEnd;
          else
            return (fromBounds.top > toBounds.top) ?
              Shapes.getBottomHandleXY(toBounds) : Shapes.getTopHandleXY(toBounds);
        },

        redraw = function() {
          if (currentEnd) {
            var end = getEnd(),

                startsAtTop = end[1] <= (fromBounds.top + fromBounds.height / 2),

                start = (startsAtTop) ?
                  Shapes.getTopHandleXY(fromBounds) : Shapes.getBottomHandleXY(fromBounds),

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
          }
        },

        getStartNode = function() {
          return fromNode;
        },

        getEndNode = function() {
          return toNode;
        },

        destroy = function() {

        };

    svgEl.appendChild(path);
    svgEl.appendChild(startHandle);
    svgEl.appendChild(endHandle);

    this.dragTo = dragTo;
    this.destroy = destroy;
    this.redraw = redraw;
    this.getStartNode = getStartNode;
    this.getEndNode = getEndNode;
  };

  return Connection;

});
