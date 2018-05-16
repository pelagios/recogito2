define([
  'common/hasEvents',
  'document/annotation/text/relations/drawing'
], function(HasEvents, Draw) {

  var TagHandle = function(label, svg) {

    var that = this,

        g = document.createElementNS(Draw.SVG_NAMESPACE, 'g'),

        rect  = document.createElementNS(Draw.SVG_NAMESPACE, 'rect'),
        text  = document.createElementNS(Draw.SVG_NAMESPACE, 'text'),
        arrow = document.createElementNS(Draw.SVG_NAMESPACE, 'path'),

        bounds,

        init = function() {
          g.setAttribute('class', 'mid');

          text.innerHTML = label;
          bounds = text.getBBox();

          text.setAttribute('dy', 3.5);
          text.setAttribute('dx', - Math.round(bounds.width / 2));

          rect.setAttribute('rx', 2); // Rounded corners
          rect.setAttribute('ry', 2);

          rect.setAttribute('width', Math.round(bounds.width) + 5);
          rect.setAttribute('height',  Math.round(bounds.height) - 2);

          arrow.setAttribute('class', 'arrow');

          rect.addEventListener('click', function() {
            that.fireEvent('click');
          });
        },

        setPosition = function(xy, orientation) {
          var x = Math.round(xy[0]) - 0.5,
              y = Math.round(xy[1]),

              dx = Math.round(bounds.width / 2),

              createArrow = function() {
                if (orientation === 'left')
                  return 'M' + (xy[0] - dx - 8) + ',' + (xy[1] - 4) + 'l-7,4l7,4';
                else if (orientation === 'right')
                  return 'M' + (xy[0] + dx + 8) + ',' + (xy[1] - 4) + 'l7,4l-7,4';
                else if (orientation === 'down')
                  return 'M' + (xy[0] - 4) + ',' + (xy[1] + 12) + 'l4,7l4,-7';
                else
                  return 'M' + (xy[0] - 4) + ',' + (xy[1] - 12) + 'l4,-7l4,7';
              };

          rect.setAttribute('x', x - 3 - dx);
          rect.setAttribute('y', y - 6.5);

          text.setAttribute('x', x);
          text.setAttribute('y', y);

          arrow.setAttribute('d', createArrow());
        },

        getLabel = function() {
          return label;
        },

        destroy = function() {
          svg.removeChild(g);
        };

    // Append first and init afterwards, so we can query text width/height
    g.appendChild(rect);
    g.appendChild(text);
    g.appendChild(arrow);
    svg.appendChild(g);

    init();

    this.getLabel = getLabel;
    this.setPosition = setPosition;
    this.destroy = destroy;

    HasEvents.apply(this);
  };
  TagHandle.prototype = Object.create(HasEvents.prototype);

  return TagHandle;

});
