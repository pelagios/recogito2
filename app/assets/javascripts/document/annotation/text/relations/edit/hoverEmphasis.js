define(['document/annotation/text/relations/bounds'], function(Bounds) {

  var HoverEmphasis = function(container, elements) {

    var bounds = Bounds.toOffsetBounds(Bounds.getUnionBounds(elements), jQuery(container)),

        outline = document.createElementNS(Bounds.SVG_NAMESPACE, 'rect'),

        init = function() {
          outline.setAttribute('x', bounds.left - 0.5);
          outline.setAttribute('y', bounds.top - 0.5);
          outline.setAttribute('width', bounds.width + 1);
          outline.setAttribute('height', bounds.height);
          outline.setAttribute('class', 'hover');
          container.appendChild(outline);
        },

        destroy = function() {
          container.removeChild(outline);
        };

    init();

    this.annotation = elements.get(0).annotation;
    this.node = { annotation: this.annotation, elements: elements };
    this.destroy = destroy;
  };

  return HoverEmphasis;

});
