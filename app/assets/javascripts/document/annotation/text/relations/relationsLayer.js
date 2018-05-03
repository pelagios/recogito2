define([
  'document/annotation/text/relations/connection',
  'document/annotation/text/relations/hoverEmphasis'
], function(Connection, HoverEmphasis) {

  var SVG_NS = "http://www.w3.org/2000/svg",

      toNode = function(e) {
        var t = jQuery(e.target).closest('.annotation');
            span = (t.length > 0) ? t[0] : undefined;

        // TODO we'll support multi-span annotations later
        if (span)
          return { annotation: span.annotation, elements: [ span ] };
      };

  var RelationsLayer = function(content, svg) {

    var contentEl = jQuery(content),

        // Current hover emphasis (regardless of whether we're currently drawing a connection)
        currentHover = false,

        // The connection currently being drawn, if any
        currentConnection = false,

        attachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.addClass('noselect');
          contentEl.on('mousedown', onMousedown);
          contentEl.on('mousemove', onMousemove);
          contentEl.on('mouseup', onMouseup);
          contentEl.on('mouseover', '.annotation', onEnterAnnotation);
          contentEl.on('mouseleave', '.annotation', onLeaveAnnotation);
        },

        detachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.removeClass('noselect');
          contentEl.off('mousedown', onMousedown);
          contentEl.off('mousemove', onMousemove);
          contentEl.off('mouseup', onMouseup);
          contentEl.off('mouseover', '.annotation', onEnterAnnotation);
          contentEl.off('mouseleave', '.annotation', onLeaveAnnotation);
        },

        /** Fire up the mouse handlers and show the SVG foreground plate **/
        show = function() {
          attachHandlers();
          svg.style.display = 'initial';
        },

        /** Clear current connection, all shapes, detach mouse handlers and hide the SVG plate **/
        hide = function() {
          currentConnection = false;

          while (svg.firstChild)
            svg.removeChild(svg.firstChild);

          detachHandlers();
          svg.style.display = 'none';
        },

        /** Drawing code for 'hover emphasis' **/
        hover = function(elements) {
          if (elements) {
            currentHover = new HoverEmphasis(svg, elements);
          } else { // Clear hover
            if (currentHover) currentHover.destroy();
            currentHover = undefined;
          }
        },

        /** Start drawing a new connection **/
        startNewConnection = function(fromNode) {
          currentConnection = new Connection(svg, fromNode);
          jQuery(document.body).css('cursor', 'none');
          render();
        },

        /** Emphasise hovered annotation **/
        onEnterAnnotation = function(e) {
          // TODO support multi-span annotations
          var t = jQuery(e.target).closest('.annotation'),
              elements = (t.length > 0) ? t : undefined;
          hover(elements);
        },

        /** Clear hover emphasis **/
        onLeaveAnnotation = function(e) {
          hover();
        },

        /** Starts a new connection **/
        onMousedown = function(e) {
          var node = toNode(e);
          if (node) startNewConnection(node);
        },

        onMousemove = function(e) {
          if (currentConnection)
            if (currentHover) currentConnection.dragTo(currentHover.asNode());
            else currentConnection.dragTo([ e.offsetX, e.offsetY ]);
        },

        /**
         * Note: we want to support both possible drawing modes: click once for start + once for
         * end; or click and hold at the start, drag to end and release.
         */
        onMouseup = function(e) {
          /*
          if (currentConnection.isComplete())
          else
            currentConnection.updateConnection(selection)
          */
        },

        render = function() {
          if (currentConnection) {
            currentConnection.redraw();
            requestAnimationFrame(render);
          }
        };

    this.show = show;
    this.hide = hide;
  };

  return RelationsLayer;

});
