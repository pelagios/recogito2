define(['document/annotation/text/relations/shapes'], function(Shapes) {

  var HoverEmphasis = function(container, elements, opts) {

    var showHandle = (opts) ? opts.showHandle : false,

        bounds = Shapes.toOffsetBounds(elements[0].getBoundingClientRect(), jQuery(container)),

        g = document.createElementNS(Shapes.SVG_NAMESPACE, 'g'),

        init = function() {
          var handleXY = Shapes.getTopHandleXY(bounds),
              outline = document.createElementNS(Shapes.SVG_NAMESPACE, 'rect'),
              handle;

          outline.setAttribute('x', bounds.left - 0.5);
          outline.setAttribute('y', bounds.top - 0.5);
          outline.setAttribute('width', bounds.width + 1);
          outline.setAttribute('height', bounds.height);

          g.setAttribute('class', 'hover');
          g.appendChild(outline);

          if (showHandle) {
            handle = document.createElementNS(Shapes.SVG_NAMESPACE, 'circle');
            handle.setAttribute('cx', handleXY[0] - 0.5);
            handle.setAttribute('cy', handleXY[1] + 0.5);
            handle.setAttribute('r', 4);
            g.appendChild(handle);
          }

          container.appendChild(g);
        },

        destroy = function() {
          container.removeChild(g);
        };

    init();

    this.annotation = elements.get(0).annotation;
    this.node = { annotation: this.annotation, elements: [ elements.get(0) ] };
    this.destroy = destroy;
  };

  return HoverEmphasis;

});
