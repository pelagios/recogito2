define(['common/hasEvents'], function(HasEvents) {

  var SVG_NAMESPACE = 'http://www.w3.org/2000/svg';

  var TagHandle = function(label) {

    var that = this,

        g = document.createElementNS(SVG_NAMESPACE, 'g'),
        rect = document.createElementNS(SVG_NAMESPACE, 'rect'),
        text = document.createElementNS(SVG_NAMESPACE, 'text'),

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

    HasEvents.apply(this);
  };
  TagHandle.prototype = Object.create(HasEvents.prototype);

  return TagHandle;

});
