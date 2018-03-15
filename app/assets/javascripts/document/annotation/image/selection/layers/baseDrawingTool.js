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

  BaseDrawingTool.prototype.drawDot = function(ctx, xy, opts) {
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

    // Inner dot (white stroke + color fill)
    ctx.beginPath();
    ctx.shadowBlur = 0;
    ctx.lineWidth = 2;
    ctx.strokeStyle = '#fff';
    ctx.fillStyle = (isHover) ? 'orange' : '#000';
    ctx.arc(xy[0], xy[1], BaseDrawingTool.HANDLE_RADIUS, 0, TWO_PI);
    ctx.fill();
    ctx.stroke();
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

  return BaseDrawingTool;

});
