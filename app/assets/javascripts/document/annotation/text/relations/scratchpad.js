define([], function() {

  var SVG_NS = "http://www.w3.org/2000/svg";

  var ScratchPad = function() {

    var svg = document.getElementsByTagName('svg')[0],

        currentLine = false,

        startLine = function(x, y) {
          var l = document.createElementNS(SVG_NS, 'line');
          l.setAttribute('x1', x - 200);
          l.setAttribute('y1', y - 179);
          l.setAttribute('x2', 200);
          l.setAttribute('y2', 200);
          l.setAttribute('marker-end', 'url(#arrow)');

          svg.appendChild(l);

          currentLine = l;
        },

        setEnd = function(x, y) {
          currentLine.setAttribute('x2', x);
          currentLine.setAttribute('y2', y);
        },

        draw = function(selection) {
          var b = selection.bounds, // shorthand
              x = Math.round(b.x + b.width / 2),
              y = Math.round(b.y + b.height / 2);

          if (currentLine) {
            setEnd(x - 200, y - 179);
            currentLine = false;
          } else {
            startLine(x, y);
          }
        };

    document.getElementById('content').addEventListener('mousemove', function(e) {
      if (currentLine)
        setEnd(e.offsetX, e.offsetY);
    });

    this.draw = draw;
  };

  return ScratchPad;

});
