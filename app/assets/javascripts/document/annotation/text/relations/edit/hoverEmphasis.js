define([
  'document/annotation/text/relations/bounds',
  'document/annotation/text/relations/drawing'
], function(Bounds, Draw) {

  var HoverEmphasis = function(svg, elements) {

    var bounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(elements), jQuery(svg)),

        outline = document.createElementNS(Draw.SVG_NAMESPACE, 'rect'),

        init = function() {
          outline.setAttribute('x', bounds.left - 0.5);
          outline.setAttribute('y', bounds.top - 0.5);
          outline.setAttribute('width', bounds.width + 1);
          outline.setAttribute('height', bounds.height);
          outline.setAttribute('class', 'hover');
          svg.appendChild(outline);
        },

        destroy = function() {
          svg.removeChild(outline);
        };

    init();

    this.annotation = elements.get(0).annotation;
    this.node = { annotation: this.annotation, elements: elements };
    this.destroy = destroy;
  };

  return HoverEmphasis;

});
