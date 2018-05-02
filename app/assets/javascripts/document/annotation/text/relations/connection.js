/** The relationship connection path **/
define([], function() {

  var SVG_NS = "http://www.w3.org/2000/svg",

      BORDER_RADIUS = 4,

      LINE_OFFSET = 10;

  var Connection = function(fromSelection, opt_toSelection) {

        // { annotation: ..., bounds: ... }
    var toSelection = opt_toSelection,

        getSelectionX = function(selection) {
          return selection.bounds.x + selection.bounds.width / 2;
        },

        getSelectionY = function(selection) {
          return selection.bounds.y + selection.bounds.height / 2;
        },

        refresh = function() {
          var deltaX = getSelectionX(toSelection) - getSelectionX(fromSelection),
              deltaY = getSelectionY(toSelection) - getSelectionY(fromSelection);

          /*
          return 'M' + start[0] + ' ' + (start[1] - 3) +
                'V' + (start[1] - delta - 0.5) +
                'A' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + (start[0] + BORDER_RADIUS) + ',' + (start[1] - LINE_OFFSET) +
                'H' + (end[0] - BORDER_RADIUS + 0.5) +
                'A' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + end[0] + ',' + (start[1] - delta) +
                'V' + (end[1] - 3);
          */
        },

        setTip = function(x, y) {

        },

        appendTo = function(svg) {

        },

        destroy = function() {

        };

    this.appendTo = appendTo;
    this.destroy = destroy;
  };

  return Connection;

});
