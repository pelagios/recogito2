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

    parseAnchor : function(anchor, opt_minheight) {
      var min_height = (opt_minheight) ? opt_minheight : 0,

          args = anchor.substring(anchor.indexOf(':') + 1).split(','),

          // TODO make this robust against change of argument order
          rx = parseInt(args[0].substring(3)),
          ry = parseInt(args[1].substring(3)),
          px = parseInt(args[2].substring(3)),
          py = parseInt(args[3].substring(3)),
          a  = parseFloat(args[4].substring(2)),
          l  = parseFloat(args[5].substring(2)),
          h  = parseFloat(args[6].substring(2)),

          height = (Math.abs(h) > min_height) ? h : min_height,

          p1 = [ px, - py ],
          p2 = [ px + Math.cos(a) * l, - py + Math.sin(a) * l],
          p3 = [ p2[0] - height * Math.sin(a), p2[1] + height * Math.cos(a) ],
          p4 = [ px - height * Math.sin(a), - py + height * Math.cos(a) ];

      return { rx: rx, ry: ry, px: px, py: py, a: a, l: l, h: h , coords: [ p1, p2, p3, p4 ]};
    },

    getAnchor : function(root, pivot, baseEnd, opposite) {
          // Baseline vector
      var dx = baseEnd[0] - pivot[0],
          dy = baseEnd[1] - pivot[1],

          // Vector base end -> opposite
          dh = [ opposite[0] - baseEnd[0], opposite[1] - baseEnd[1] ],

          corr = (dx < 0 && dy >= 0) ? Math.PI : ((dx < 0 && dy <0) ? - Math.PI : 0),

          baselineAngle = Math.atan(dy / dx) + corr,
          baselineLength = Math.sqrt(dx * dx + dy * dy),
          height = Math.sqrt(dh[0] * dh[0] + dh[1] * dh[1]);

      if (corr === 0 && dh[1] < 0)
        height = - height;
      else if (corr < 0 && dh[0] < 0)
        height = - height;
      else if (corr > 0 && dh[0] > 0)
        height = - height;

      return 'lbox:' +
        'rx=' + Math.round(root[0]) + ',' +
        'ry=' + Math.round(Math.abs(root[1])) + ',' +
        'px=' + Math.round(pivot[0]) + ',' +
        'py=' + Math.round(Math.abs(pivot[1])) + ',' +
        'a=' + baselineAngle + ',' +
        'l=' + Math.round(baselineLength) + ',' +
        'h=' + Math.round(height);
    },

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
