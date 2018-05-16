define([
  'document/annotation/text/relations/bounds',
  'document/annotation/text/relations/drawing'
], function(Bounds, Draw) {


  var HoverEmphasis = function(svg, elements) {

    var outlines = document.createElementNS(Draw.SVG_NAMESPACE, 'g'),

        init = function() {
          var bounds = new Bounds(elements, jQuery(svg)),

              svgRects = bounds.rects.map(function(r) {
                var svgRect = document.createElementNS(Draw.SVG_NAMESPACE, 'rect');
                svgRect.setAttribute('x', r.left - 0.5);
                svgRect.setAttribute('y', r.top - 0.5);
                svgRect.setAttribute('width', r.width + 1);
                svgRect.setAttribute('height', r.height);
                svgRect.setAttribute('class', 'hover');
                return svgRect;
              });

          svgRects.forEach(function(r) { outlines.appendChild(r); });
          svg.appendChild(outlines);
        },

        destroy = function() {
          svg.removeChild(outlines);
        };

    init();

    this.annotation = elements.get(0).annotation;
    this.node = { annotation: this.annotation, elements: elements };
    this.destroy = destroy;
  };

  return HoverEmphasis;

});
