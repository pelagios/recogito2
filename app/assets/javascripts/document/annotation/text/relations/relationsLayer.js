define([], function() {

  var SVG_NS = "http://www.w3.org/2000/svg",

      BORDER_RADIUS = 4,

      LINE_OFFSET = 10;

  var RelationsLayer = function(content, svg) {

    var contentEl = jQuery(content),

        enabled = false, // Buffering this, so we don't access DOM each time

        mouseX, mouseY,

        // { path: ..., selection: ... }
        currentPath = false,

        isOver = false,

        attachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.on('mousemove', onMousemove);
          contentEl.on('mouseover', '.annotation', onEnterAnnotation);
          contentEl.on('mouseleave', '.annotation', onLeaveAnnotation);
        },

        detachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.off('mousemove', onMousemove);
          contentEl.off('mouseover', '.annotation', onEnterAnnotation);
          contentEl.off('mouseleave', '.annotation', onLeaveAnnotation);
        },

        show = function() {
          enabled = true;
          attachHandlers();
          svg.style.display = 'initial';
        },

        hide = function() {
          enabled = false;

          currentPath = false;
          while (svg.firstChild)
            svg.removeChild(svg.firstChild);

          detachHandlers();
          svg.style.display = 'none';
        },

        isEnabled = function() {
          return enabled;
        },

        getHandleXY = function(fromSelection) {
          var bounds = fromSelection.bounds, // shorthand
              startX = Math.round(bounds.x + bounds.width / 2) - 199.5,
              startY = Math.round(bounds.y) - jQuery(svg).offset().top + jQuery(window).scrollTop();

          return [ startX, startY ];
        },

        getHandleElement = function(el) {
          var rect = el.get(0).getBoundingClientRect(),
              x = Math.round(rect.x + rect.width / 2) - 199.5,
              y = Math.round(rect.y) - jQuery(svg).offset().top + jQuery(window).scrollTop();

          return [ x, y ];
        },

        computePath = function(fromSelection, opt_destination) {
          var start = getHandleXY(fromSelection),
              end = (opt_destination) ? getHandleElement(opt_destination) : [ mouseX, mouseY ],
              delta = LINE_OFFSET - BORDER_RADIUS;

          return 'M' + start[0] + ' ' + (start[1] - 3) +
            'V' + (start[1] - delta - 0.5) +
            'A' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + (start[0] + BORDER_RADIUS) + ',' + (start[1] - LINE_OFFSET) +
            'H' + (end[0] - BORDER_RADIUS + 0.5) +
            'A' + BORDER_RADIUS + ',' + BORDER_RADIUS + ' 0 0 1 ' + end[0] + ',' + (start[1] - delta) +
            'V' + (end[1] - 3);
        },

        initPath = function(fromSelection) {
          var start = getHandleXY(fromSelection),
              startCircle = document.createElementNS(SVG_NS, 'circle'),
              endCircle = document.createElementNS(SVG_NS, 'circle'),
              path = document.createElementNS(SVG_NS, 'path');

          startCircle.setAttribute('cx', start[0]);
          startCircle.setAttribute('cy', start[1]);
          startCircle.setAttribute('r', 4);

          endCircle.setAttribute('cx', start[0]);
          endCircle.setAttribute('cy', start[1]);
          endCircle.setAttribute('r', 4);
          endCircle.setAttribute('class', 'end');

          path.setAttribute('d', computePath(fromSelection));

          svg.appendChild(startCircle);
          svg.appendChild(endCircle);
          svg.appendChild(path);

          currentPath = { path: path, end: endCircle, selection: fromSelection };
          render();
        },

        updatePath = function(destination) {
          var end = (destination) ? getHandleElement(destination) : [ mouseX, mouseY ];

          currentPath.path.setAttribute('d', computePath(currentPath.selection, destination));
          currentPath.end.setAttribute('cx', end[0]);
          currentPath.end.setAttribute('cy', end[1]);
        },

        render = function() {
          if (currentPath) {
            if (!isOver) updatePath();
            requestAnimationFrame(render);
          }
        },

        onMousemove = function(e) {
          mouseX = e.offsetX;
          mouseY = e.offsetY;
        },

        onEnterAnnotation = function(e) {
          isOver = true;
          if (currentPath)
            updatePath(jQuery(e.target).closest('.annotation'));
        },

        onLeaveAnnotation = function(e) {
          isOver = false;
        },

        select = function(selection) {
          if (selection) {
            initPath(selection);
          }
        };

    this.show = show;
    this.hide = hide;
    this.select = select;
    this.isEnabled = isEnabled;
  };

  return RelationsLayer;

});
