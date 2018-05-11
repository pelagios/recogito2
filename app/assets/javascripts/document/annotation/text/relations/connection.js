/** A connection is an on-screen representation of a relation **/
define(['document/annotation/text/relations/bounds'], function(Bounds) {

  var MidHandle = function(label) {

    var g = document.createElementNS(Bounds.SVG_NAMESPACE, 'g'),
        rect = document.createElementNS(Bounds.SVG_NAMESPACE, 'rect'),
        text = document.createElementNS(Bounds.SVG_NAMESPACE, 'text'),

        init = function() {
          var bbox;

          g.setAttribute('class', 'mid');

          text.setAttribute('dy', 3.5);
          text.innerHTML = label;
          bbox = text.getBBox();

          rect.setAttribute('rx', 2);
          rect.setAttribute('ry', 2);
          rect.setAttribute('width', Math.round(bbox.width) + 4);
          rect.setAttribute('height',  Math.round(bbox.height) - 3);
        },

        setX = function(x) {
          var xr = Math.round(x) - 0.5;
          rect.setAttribute('x', xr - 3);
          text.setAttribute('x', xr);
        },

        setY = function(y) {
          rect.setAttribute('y', y - 6);
          text.setAttribute('y', y);
        },

        appendTo = function(svg) {
          svg.appendChild(g);
          init();
        };

    g.appendChild(rect);
    g.appendChild(text);

    this.g = g;
    this.setX = setX;
    this.setY = setY;
    this.appendTo = appendTo;
  };

  var Relation = function(svgEl, fromNode, opt_toNode) {

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
        path        = document.createElementNS(Bounds.SVG_NAMESPACE, 'path'),
        startHandle = document.createElementNS(Bounds.SVG_NAMESPACE, 'circle'),
        endHandle   = document.createElementNS(Bounds.SVG_NAMESPACE, 'circle'),

        midHandle, //    = new MidHandle(),

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

                d = Bounds.LINE_DISTANCE - Bounds.BORDER_RADIUS, // Shorthand: vertical straight line length

                // Path that starts at the top edge of the annotation highlight
                compileBottomPath = function() {
                  var arc1 = (deltaX > 0) ? Bounds.ARC_9CC : Bounds.ARC_3CW,
                      arc2 = (deltaX > 0) ? Bounds.ARC_0CW : Bounds.ARC_0CC;

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] + half - Math.abs(deltaX) + Bounds.LINE_DISTANCE :
                    start[1] + Bounds.LINE_DISTANCE;

                  return 'M' + start[0] +
                         ' ' + start[1] +
                         'v' + d +
                         arc1 +
                         'h' + (deltaX - 2 * Math.sign(deltaX) * Bounds.BORDER_RADIUS) +
                         arc2 +
                         'V' + end[1];
                },

                // Path that starts at the bottom edge of the annotation highlight
                compileTopPath = function() {
                  var arc1 = (deltaX > 0) ? Bounds.ARC_9CW : Bounds.ARC_3CC,
                      arc2 = (deltaX > 0) ?
                        (deltaY >= 0) ? Bounds.ARC_0CW : Bounds.ARC_6CC :
                        (deltaY >= 0) ? Bounds.ARC_0CC : Bounds.ARC_6CW;

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] - (half - Math.abs(deltaX)) - Bounds.LINE_DISTANCE :
                    start[1] - Bounds.LINE_DISTANCE;

                  return 'M' + start[0] +
                         ' ' + start[1] +
                         'v-' + (Bounds.LINE_DISTANCE - Bounds.BORDER_RADIUS) +
                         arc1 +
                         'h' + (deltaX - 2 * Math.sign(deltaX) * Bounds.BORDER_RADIUS) +
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
          midHandle = new MidHandle(label);
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

  return Relation;

});
