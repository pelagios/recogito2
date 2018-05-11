/** A connection is an on-screen representation of a relation **/
define([
  'document/annotation/text/relations/bounds',
  'document/annotation/text/relations/tagging/tagHandle'
], function(Bounds, TagHandle) {

  var SVG_NAMESPACE = 'http://www.w3.org/2000/svg',

      // Rounded corner arc radius
      BORDER_RADIUS = 3,

      // Horizontal distance between connection line and annotation highlight
      LINE_DISTANCE = 6.5,

      // Possible rounded corner SVG arc configurations: clock position + clockwise/counterclockwise
      ARC_0CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_0CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_3CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_3CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_6CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_6CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_9CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_9CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',' + BORDER_RADIUS;

  var Connection = function(svgEl, fromNode, opt_toNode) {

    var that = this,

        svg = jQuery(svgEl), // shorthand

        // { annotation: ..., elements: ... }
        toNode = opt_toNode,

        toBounds = (opt_toNode) ? Bounds.toOffsetBounds(Bounds.getUnionBounds(opt_toNode.elements), svg) : undefined,

        // Note that the selection bounds are useless after scrolling or resize. They
        // represent viewport bounds at the time of selection, so we store document
        // offsets instead
        fromBounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(fromNode.elements), svg),

        // SVG elements
        path        = document.createElementNS(SVG_NAMESPACE, 'path'),
        startHandle = document.createElementNS(SVG_NAMESPACE, 'circle'),
        endHandle   = document.createElementNS(SVG_NAMESPACE, 'circle'),

        midHandle,

        // [x,y] array or node object
        currentEnd = opt_toNode,

        // [x,y]
        currentMidXY,

        // Flag indicating whether the relation is completed (or still drawing)
        attached = false,

        dragTo = function(xyOrNode) {
          currentEnd = xyOrNode;

          if (xyOrNode.elements) {
            toNode = xyOrNode;
            toBounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(xyOrNode.elements), svg);
          }
        },

        attach = function() {
          if (currentEnd.elements) {
            attached = true;
            // svgEl.appendChild(midHandle);
          }
        },

        isAttached = function() {
          return attached;
        },

        getEnd = function() {
          if (currentEnd instanceof Array)
            return currentEnd;
          else
            return (fromBounds.top > toBounds.top) ?
              Bounds.getBottomHandleXY(toBounds) : Bounds.getTopHandleXY(toBounds);
        },

        redraw = function() {
          if (currentEnd) {
            var end = getEnd(),

                startsAtTop = end[1] <= (fromBounds.top + fromBounds.height / 2),

                start = (startsAtTop) ?
                  Bounds.getTopHandleXY(fromBounds) : Bounds.getBottomHandleXY(fromBounds),

                deltaX = end[0] - start[0],
                deltaY = end[1] - start[1],

                half = (Math.abs(deltaX) + Math.abs(deltaY)) / 2, // Half of length, for middot pos computation
                midX = (half > Math.abs(deltaX)) ? start[0] + deltaX : start[0] + half * Math.sign(deltaX),
                midY, // computed later

                d = LINE_DISTANCE - BORDER_RADIUS, // Shorthand: vertical straight line length

                // Path that starts at the top edge of the annotation highlight
                compileBottomPath = function() {
                  var arc1 = (deltaX > 0) ? ARC_9CC : ARC_3CW,
                      arc2 = (deltaX > 0) ? ARC_0CW : ARC_0CC;

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] + half - Math.abs(deltaX) + LINE_DISTANCE :
                    start[1] + LINE_DISTANCE;

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

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] - (half - Math.abs(deltaX)) - LINE_DISTANCE :
                    start[1] - LINE_DISTANCE;

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
            startHandle.setAttribute('r', 2);
            startHandle.setAttribute('class', 'start');

            endHandle.setAttribute('cx', end[0]);
            endHandle.setAttribute('cy', end[1]);
            endHandle.setAttribute('r', 2);
            endHandle.setAttribute('class', 'end');

            if (startsAtTop) path.setAttribute('d', compileTopPath());
            else path.setAttribute('d', compileBottomPath());

            currentMidXY = [ midX, midY ];

            if (midHandle) {
              midHandle.setX(midX);
              midHandle.setY(midY);
            }
          }
        },

        getStartNode = function() {
          return fromNode;
        },

        getEndNode = function() {
          return toNode;
        },

        getMidXY = function() {
          return currentMidXY;
        },

        recompute = function() {
          fromBounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(fromNode.elements), svg);
          if (currentEnd && currentEnd.elements)
            toBounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(opt_toNode.elements), svg);
          redraw();
        },

        setLabel = function(label) {
          midHandle = new TagHandle(label);
          midHandle.appendTo(svgEl);
        },

        destroy = function() {

          // TODO implement

        };

    svgEl.appendChild(path);
    svgEl.appendChild(startHandle);
    svgEl.appendChild(endHandle);

    this.dragTo = dragTo;
    this.attach = attach;
    this.isAttached = isAttached;
    this.recompute = recompute;
    this.destroy = destroy;
    this.redraw = redraw;
    this.setLabel = setLabel;
    this.getStartNode = getStartNode;
    this.getEndNode = getEndNode;
    this.getMidXY = getMidXY;
  };

  // Make static vars visible to outside
  Connection.BORDER_RADIUS = BORDER_RADIUS;
  Connection.LINE_DISTANCE = LINE_DISTANCE;

  Connection.ARC_0CW = ARC_0CW;
  Connection.ARC_0CC = ARC_0CC;
  Connection.ARC_3CW = ARC_3CW;
  Connection.ARC_3CC = ARC_3CC;
  Connection.ARC_6CW = ARC_6CW;
  Connection.ARC_6CC = ARC_6CC;
  Connection.ARC_9CW = ARC_9CW;
  Connection.ARC_9CC = ARC_9CC;

  return Connection;

});
