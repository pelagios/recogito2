define([
  'common/config',
  'document/annotation/image/selection/layers/geom2D',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Config, Geom2D, Layer, Style) {

      /** Shorthand **/
  var TWO_PI = 2 * Math.PI,

      /** Constants **/
      MIN_DRAG_TIME = 300,   // Minimum duration of an annotation drag (milliseconds)
      MIN_LINE_LENGTH = 10;  // Minimum length of a baseline

  var TiltedBoxDrawingTool = function(containerEl, olMap) {

    var self = this,

        canvas = (function() {
          var canvas = jQuery('<canvas class="tiltedbox-drawing"></canvas>');
          canvas.hide();
          jQuery(containerEl).append(canvas);
          return canvas[0];
        })(),

        ctx = canvas.getContext('2d'),

        /** Painting state flags **/
        painting = false,
        extrude = false,

        /** Parameters of the current drawing **/
        anchorX,       // Anchor point
        anchorY,
        baseEndX,      // Baseline end
        baseEndY,
        oppositeX,     // Coordinate diagonally opposite the anchor point
        oppositeY,
        lastClickTime, // Time of last mousedown event

        attachMouseHandlers = function() {
          // Handlers on the drawing canvas
          var c = jQuery(canvas);
          c.mousedown(onMouseDown);
          c.mousemove(onMouseMove);
          c.mouseup(onMouseUp);
        },

        updateSize = function() {
          var c = jQuery(canvas);
          canvas.width = c.width();
          canvas.height = c.height();
          ctx.strokeStyle = Style.COLOR_RED;
          ctx.lineWidth = Style.BOX_BASELINE_WIDTH;
        },

        clearCanvas = function() {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
        },

        startPainting = function(e) {
          painting = true;
          anchorX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
          anchorY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;

          // Paints a redundant line of 0px length - but saves a few duplicate lines of code
          paintBaseline(e);
        },

        paintBaseline = function(e) {
          var offsetX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX,
              offsetY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;

          ctx.fillStyle = Style.COLOR_RED;
          ctx.beginPath();
          ctx.arc(anchorX, anchorY, Style.BOX_ANCHORDOT_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();

          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(offsetX, offsetY);
          ctx.stroke();
          ctx.closePath();
        },

        startExtruding = function(e) {
          baseEndX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX;
          baseEndY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY;

          if (Geom2D.len(anchorX, anchorY, baseEndX, baseEndY) > MIN_LINE_LENGTH) {
            extrude = true;
          } else {
            // Reject lines that are too short
            painting = false;
            clearCanvas();
          }
        },

        paintAnnotation = function(e) {
              // Baseline vector (start to end)
          var delta = [ (baseEndX - anchorX), (baseEndY - anchorY) ],

              // Slope of the baseline normal
              normal = Geom2D.normalize([-1 * delta[1], delta[0]]),

              // Vector baseline->mouse
              offsetX = (e.offsetX) ? e.offsetX : e.originalEvent.layerX,
              offsetY = (e.offsetY) ? e.offsetY : e.originalEvent.layerY,
              toMouse = [ offsetX - baseEndX, offsetY - baseEndY ],

              // Projection of toMouse onto normal
              f = [
                normal[0] * Geom2D.len(toMouse) *
                  Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse))),
                normal[1] * Geom2D.len(toMouse) *
                  Math.cos(Geom2D.angleBetween(normal, Geom2D.normalize(toMouse)))
              ];

          oppositeX = baseEndX + f[0];
          oppositeY = baseEndY + f[1];

          ctx.globalAlpha = Style.BOX_FILL_OPACITY;
          ctx.fillStyle = Style.COLOR_RED;
          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(anchorX + f[0], anchorY + f[1]);
          ctx.lineTo(oppositeX, oppositeY);
          ctx.lineTo(baseEndX, baseEndY);
          ctx.fill();
          ctx.closePath();
          ctx.globalAlpha = 1;

          // Finished baseline
          ctx.beginPath();
          ctx.arc(anchorX, anchorY, Style.BOX_ANCHORDOT_RADIUS, 0, TWO_PI);
          ctx.fill();
          ctx.closePath();

          ctx.beginPath();
          ctx.moveTo(anchorX, anchorY);
          ctx.lineTo(baseEndX, baseEndY);
          ctx.stroke();
          ctx.closePath();
        },

        finalizeAnnotation = function(e, callback) {
          var annotationStub = {
                annotates: {
                  document_id: Config.documentId,
                  filepart_id: Config.partId,
                  content_type: Config.contentType
                },
                bodies: []
              },

              imageAnchorCoords = olMap.getCoordinateFromPixel([ anchorX, anchorY ]),
              imageEndCoords = olMap.getCoordinateFromPixel([ baseEndX, baseEndY ]),
              imageOppositeCoords = olMap.getCoordinateFromPixel([ oppositeX, oppositeY ]),

              dx = imageEndCoords[0] - imageAnchorCoords[0],
              dy = imageEndCoords[1] - imageAnchorCoords[1],
              dh = [
                imageOppositeCoords[0] - imageEndCoords[0],
                imageOppositeCoords[1] - imageEndCoords[1]
              ],

              corr = (dx < 0 && dy >= 0) ? Math.PI : ((dx < 0 && dy <0) ? - Math.PI : 0),

              baselineAngle = Math.atan(dy / dx) + corr,
              baselineLength = Math.sqrt(dx * dx + dy * dy),
              height = Math.sqrt(dh[0]*dh[0] + dh[1]*dh[1]);

          if (corr === 0 && dh[1] < 0)
            height = -1 * height;
          else if (corr < 0 && dh[0] < 0)
            height = -1 * height;
          else if (corr > 0 && dh[0] > 0)
            height = -1 * height;

          // Reset state
          extrude = false;
          painting = false;

          annotationStub.anchor ='tbox:' +
            'x=' + Math.round(imageAnchorCoords[0]) + ',' +
            'y=' + Math.round(Math.abs(imageAnchorCoords[1])) + ',' +
            'a=' + baselineAngle + ',' +
            'l=' + Math.round(baselineLength) + ',' +
            'h=' + Math.round(height);

          self.fireEvent('newSelection', {
            isNew      : true,
            annotation : annotationStub,
            mapBounds  : self.pointToBounds(imageAnchorCoords)
          });
        },

        onMouseDown = function(e) {
          lastClickTime = new Date().getTime();
           if (extrude)
             // Extrude phase is where box height is defined - click means annotation is done
             finalizeAnnotation(e);
           else
             startPainting(e);
        },

        onMouseMove = function(e) {
          if (painting) {
            clearCanvas();
            if (extrude)
              paintAnnotation(e);
            else
              paintBaseline(e);
          }
        },

        onMouseUp = function(e) {
          var now = new Date().getTime();

          if (painting) {
            if ((now - lastClickTime) < MIN_DRAG_TIME) {
              // Single click - just clear canvas and ignore
              painting = false;
              clearCanvas();
            } else {
              startExtruding(e);
            }
          }
        },

        createNewSelection = function(e, callback) {
          // The tilted-box drawing tool works differently - it overlays a drawing
          // canvas on .setEnabled(true) handles it's own mouse events
          // I.e. we can ignore calls to this method.
        },

        clearSelection = function() {
          clearCanvas();
        },

        setEnabled = function(enabled) {
          if (enabled) jQuery(canvas).show(); else jQuery(canvas).hide();
        };

    attachMouseHandlers();
    updateSize();

    // Reset canvas on window resize
    jQuery(window).on('resize', updateSize);

    this.createNewSelection = createNewSelection;
    this.clearSelection = clearSelection;
    this.setEnabled = setEnabled;
    this.updateSize = updateSize;

    Layer.apply(this, [ olMap ]);
  };
  TiltedBoxDrawingTool.prototype = Object.create(Layer.prototype);

  return TiltedBoxDrawingTool;

});
