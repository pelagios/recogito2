/** The relationship connection path **/
define([], function() {

  var SVG_NS = "http://www.w3.org/2000/svg",

      BORDER_RADIUS = 4,

      LINE_OFFSET = 8,

      ARC_0CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',' + BORDER_RADIUS,
      ARC_0CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',' + BORDER_RADIUS,

      ARC_3CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,

      ARC_6CC = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 0 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS,
      ARC_6CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 -' + BORDER_RADIUS + ',-' + BORDER_RADIUS,

      ARC_9CW = 'a' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + BORDER_RADIUS + ',-' + BORDER_RADIUS;

  var Connection = function(svgEl, fromSelection, opt_toSelection) {

        // { annotation: ..., bounds: ... }
    var toSelection = opt_toSelection,

        svg = jQuery(svgEl),

        path = document.createElementNS(SVG_NS, 'path'),

        getSelectionMiddle = function(selection) {
          return selection.bounds.x + selection.bounds.width / 2;
        },

        getSelectionTop = function(selection) {
          return selection.bounds.y;
        },

        getStart = function(mousePos) {
          // TODO make sensitive to mouse pos
          return [ getSelectionMiddle(fromSelection), getSelectionTop(fromSelection) ];
        },

        computePath = function(mousePos) {
          var offset = svg.offset(),

              deltaX = mousePos[0] - getSelectionMiddle(fromSelection) + offset.left,
              deltaY = mousePos[1] - getSelectionTop(fromSelection) + offset.top,

              arc1 = (deltaX < 0) ? ARC_3CC : ARC_9CW,
              arc2 = (deltaX < 0) ?
                (deltaY <= 0) ? ARC_6CW : ARC_0CC :
                (deltaY <= 0) ? ARC_6CC : ARC_0CW,

              start = getStart(mousePos);

          return 'M' + (start[0] - offset.left - 0.5) + ' ' + (start[1] - offset.top - 0.5) +
                 'v-' + (LINE_OFFSET - BORDER_RADIUS) +
                 arc1 +
                 'h' + (deltaX - 2 * BORDER_RADIUS) +
                 arc2 +
                 'V' + mousePos[1];
        },

        refresh = function(mousePos) {
          var d = computePath(mousePos);
          path.setAttribute('d', d);
        },

        setEnd = function(x, y) {

        },

        destroy = function() {

        };

    svgEl.appendChild(path);

    this.refresh = refresh;
    this.destroy = destroy;
  };

  return Connection;

});
