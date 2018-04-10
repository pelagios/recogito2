/**
 * Utility methods for handling/drawing PlaceFlag shapes.
 */
define(['document/annotation/image/selection/layers/baseDrawingTool'], function(BaseTool) {

  var TWO_PI = 2 * Math.PI,

      drawLine = function(ctx, from, to, hover) {
        var line = function() {
              ctx.beginPath();
              ctx.moveTo(from[0], from[1]);
              ctx.lineTo(to[0], to[1]);
              ctx.stroke();
              ctx.closePath();
            };

        ctx.lineWidth = 4.4;
        ctx.strokeStyle = '#000';
        line(ctx, from, to);

        ctx.lineWidth = 2.8;
        ctx.strokeStyle = (hover) ? BaseTool.HOVER_COLOR : '#fff';
        line(ctx, from, to);
      },

      drawDot = function(ctx, xy, radius, isHover) {
       ctx.beginPath();
       ctx.lineWidth = 0.8;
       ctx.shadowBlur = (isHover) ? 6 : 0;
       ctx.strokeStyle = '#000';
       ctx.fillStyle = (isHover) ? BaseTool.HOVER_COLOR : '#fff';
       ctx.arc(xy[0], xy[1], radius, 0, TWO_PI);
       ctx.fill();
       ctx.stroke();
       ctx.shadowBlur = 0;
       ctx.closePath();
     },

     drawBox = function(ctx, anchor, baseEnd, opposite, hover) {
           // Vector baseEnd -> opposite
       var fx = opposite[0] - baseEnd[0],
           fy = opposite[1] - baseEnd[1];

       ctx.lineWidth = 0.9;
       ctx.strokeStyle = '#000';

       ctx.beginPath();
       ctx.moveTo(anchor[0], anchor[1]);
       ctx.lineTo(anchor[0] + fx, anchor[1] + fy);
       ctx.lineTo(opposite[0], opposite[1]);
       ctx.lineTo(baseEnd[0], baseEnd[1]);
       ctx.stroke();
       ctx.closePath();

       drawDot(ctx, opposite, 5, hover === 'OPPOSITE_HANDLE');
     };

  // Public methods
  return {

    renderTether : function(ctx, root, pivot, hover) {
      var nook = function(inset) {
            var x = (inset) ? pivot[0] : pivot[0] - 1,
                y = (inset) ? pivot[1] : pivot[1] + 1,
                l = (inset) ? 34 : 35;

            ctx.beginPath();
            ctx.moveTo(x, y - l);
            ctx.lineTo(x, y);
            ctx.lineTo(x + l, y);
            ctx.stroke();
            ctx.closePath();
          };

      ctx.lineWidth = 0.8;
      ctx.strokeStyle = '#000';
      ctx.beginPath();
      ctx.moveTo(root[0], root[1]);
      ctx.lineTo(pivot[0], pivot[1]);
      ctx.stroke();
      ctx.closePath();

      drawDot(ctx, pivot, 5, hover === 'SHAPE');
      BaseTool.drawHandle(ctx, root, { hover: hover === 'ROOT' });
    },

    renderBaseline : function(ctx, root, pivot, baseEnd, hover) {
      drawLine(ctx, pivot, baseEnd, hover === 'SHAPE'); // Baseline
      this.renderTether(ctx, root, pivot, hover); // Root point, tether + tether handle
      drawDot(ctx, baseEnd, 5, hover === 'BASE_END_HANDLE'); // Baseline end handle
    },

    renderShape : function(ctx, root, pivot, baseEnd, opposite, hover) {
      drawBox(ctx, pivot, baseEnd, opposite, hover);
      this.renderBaseline(ctx, root, pivot, baseEnd, hover);
    }

  };

});
