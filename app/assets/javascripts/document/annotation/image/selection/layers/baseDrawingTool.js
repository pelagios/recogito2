define([
  'common/config',
  'common/hasEvents'
], function(Config, HasEvents) {

  /** Constants **/
  var TWO_PI = 2 * Math.PI;

  var BaseDrawingTool = function(olMap) {
    this.olMap = olMap;
    HasEvents.apply(this);
  };
  BaseDrawingTool.prototype = Object.create(HasEvents.prototype);

  BaseDrawingTool.prototype.imageToCanvas = function(xy) {
    return this.olMap.getPixelFromCoordinate([xy[0], xy[1]])
      .map(function(v) { return Math.round(v); });
  };

  BaseDrawingTool.prototype.canvasToImage = function(xy) {
    return this.olMap.getCoordinateFromPixel([xy[0], xy[1]])
      .map(function(v) { return Math.round(v); });
  };

  BaseDrawingTool.prototype.crossFill = function(diff, destination, canvasKeyOrArray, imageKey) {
    var that = this,

        crossFillOne = function(cKey, iKey) {
          if (diff[cKey] || diff[iKey]) {
            if (diff[cKey] && !diff[iKey])
              // Cross-fill image coordinate from canvas coordinate
              destination[iKey] = that.canvasToImage(diff[cKey]);

            else if (diff[iKey] && !diff[cKey])
              destination[cKey] = that.imageToCanvas(diff[iKey]);
          }
        };

    if (jQuery.isArray(canvasKeyOrArray)) {
      canvasKeyOrArray.map(function(tuple) {
        crossFillOne(tuple[0], tuple[1]);
      });
    } else {
      crossFillOne(canvasKeyOrArray, imageKey);
    }
  };

  BaseDrawingTool.prototype.drawHandle = function(ctx, xy, opts) {
    var hasBlur = (opts) ? opts.blur || opts.hover : false, // Hover implies blur
        isHover = (opts) ? opts.hover : false;

    // Black Outline
    ctx.beginPath();
    ctx.lineWidth = 4;
    ctx.shadowBlur = (hasBlur) ? 6 : 0;
    ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.8)';
    ctx.arc(xy[0], xy[1], BaseDrawingTool.HANDLE_RADIUS, 0, TWO_PI);
    ctx.stroke();
    ctx.closePath();

    // Inner dot (white stroke + color fill)
    ctx.beginPath();
    ctx.shadowBlur = 0;
    ctx.lineWidth = 2;
    ctx.strokeStyle = '#fff';
    ctx.fillStyle = (isHover) ? BaseDrawingTool.HOVER_COLOR  : '#000';
    ctx.arc(xy[0], xy[1], BaseDrawingTool.HANDLE_RADIUS, 0, TWO_PI);
    ctx.fill();
    ctx.stroke();
    ctx.closePath();
  };

  BaseDrawingTool.prototype.buildSelection = function(anchor, canvasBounds, imageBounds) {
    var annotation = {
          annotates: {
            document_id: Config.documentId,
            filepart_id: Config.partId,
            content_type: Config.contentType
          },
          anchor: anchor,
          bodies: []
        };

    return {
      annotation  : annotation,
      canvasBounds: canvasBounds,
      imageBounds : imageBounds
    };
  };

  BaseDrawingTool.HANDLE_RADIUS = 6;
  BaseDrawingTool.HOVER_COLOR = 'orange';

  return BaseDrawingTool;

});
