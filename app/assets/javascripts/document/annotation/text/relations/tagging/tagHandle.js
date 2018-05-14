define([
  'common/hasEvents',
  'document/annotation/text/relations/drawing'
], function(HasEvents, Draw) {

  var TagHandle = function(label) {

    var that = this,

        svg,

        g = document.createElementNS(Draw.SVG_NAMESPACE, 'g'),
        rect = document.createElementNS(Draw.SVG_NAMESPACE, 'rect'),
        text = document.createElementNS(Draw.SVG_NAMESPACE, 'text'),

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

          rect.addEventListener('click', function() {
            that.fireEvent('click');
          });
        },

        setXY = function(xy) {
          var x = Math.round(xy[0]) - 0.5,
              y = Math.round(xy[1]);

          rect.setAttribute('x', x - 3);
          rect.setAttribute('y', y - 6);

          text.setAttribute('x', x);
          text.setAttribute('y', y);
        },

        getLabel = function() {
          return label;
        },

        appendTo = function(svgEl) {
          svg = svgEl;
          svg.appendChild(g);
          init();
        },

        destroy = function() {
          if (svg) svg.removeChild(g);
        };

    g.appendChild(rect);
    g.appendChild(text);

    this.setXY = setXY;
    this.getLabel = getLabel;
    this.appendTo = appendTo;
    this.destroy = destroy;

    HasEvents.apply(this);
  };
  TagHandle.prototype = Object.create(HasEvents.prototype);

  return TagHandle;

});
