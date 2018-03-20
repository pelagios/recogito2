define(['document/annotation/image/selection/layers/baseDrawingTool'], function(BaseTool) {

      /** Constants **/
  var TWO_PI = 2 * Math.PI,

      SMALL_HANDLE_RADIUS = 5,

     renderSmallHandle = function(ctx, xy, isHover) {
        ctx.beginPath();
        ctx.lineWidth = 0.8;
        ctx.shadowBlur = (isHover) ? 6 : 0;
        ctx.strokeStyle = '#000';
        ctx.fillStyle = (isHover) ? BaseTool.HOVER_COLOR : '#fff';
        ctx.arc(xy[0], xy[1], SMALL_HANDLE_RADIUS, 0, TWO_PI);
        ctx.fill();
        ctx.stroke();
        ctx.shadowBlur = 0;
        ctx.closePath();
      },

      renderBox = function(ctx, anchor, baseEnd, opposite, hover) {
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

        renderSmallHandle(ctx, opposite, hover === 'OPPOSITE_HANDLE');
      };

  // Public methods
  return {

    SMALL_HANDLE_RADIUS : SMALL_HANDLE_RADIUS,

    parseAnchor : function(anchor, opt_minheight) {
      var min_height = (opt_minheight) ? opt_minheight : 0,

          args = anchor.substring(anchor.indexOf(':') + 1).split(','),

          // TODO make this robust against change of argument order
          x = parseInt(args[0].substring(2)),
          y = - parseInt(args[1].substring(2)),
          a = parseFloat(args[2].substring(2)),
          l = parseFloat(args[3].substring(2)),
          h = parseFloat(args[4].substring(2)),

          height = (Math.abs(h) > min_height) ? h : min_height,

          p1 = [ x, y ],
          p2 = [ x + Math.cos(a) * l, y + Math.sin(a) * l ],
          p3 = [ p2[0] - height * Math.sin(a), p2[1] + height * Math.cos(a) ],
          p4 = [ x - height * Math.sin(a), y + height * Math.cos(a) ];

      return { x: x, y: y, a: a, l: l, h: h , coords: [ p1, p2, p3, p4 ]};
    },

    getAnchor : function(pivot, baseEnd, opposite) {
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

      return 'tbox:' +
        'x=' + Math.round(pivot[0]) + ',' +
        'y=' + Math.round(Math.abs(pivot[1])) + ',' +
        'a=' + baselineAngle + ',' +
        'l=' + Math.round(baselineLength) + ',' +
        'h=' + Math.round(height);
    },

    renderBaseline : function(ctx, anchor, baseEnd, hover) {
      var line = function(from, to) {
            ctx.beginPath();
            ctx.moveTo(from[0], from[1]);
            ctx.lineTo(to[0], to[1]);
            ctx.stroke();
            ctx.closePath();
          },

          isHover = hover === 'SHAPE' || hover === 'ANCHOR';

      ctx.lineWidth = 4.4;
      ctx.strokeStyle = '#000';
      line(anchor, baseEnd);

      ctx.lineWidth = 2.8;
      ctx.strokeStyle = (isHover) ? BaseTool.HOVER_COLOR : '#fff';
      line(anchor, baseEnd);

      renderSmallHandle(ctx, baseEnd, hover === 'BASE_END_HANDLE');
      BaseTool.drawHandle(ctx, anchor, { hover: hover === 'SHAPE' });
    },

    renderShape : function(ctx, anchor, baseEnd, opposite, hover) {
      renderBox(ctx, anchor, baseEnd, opposite, hover);
      this.renderBaseline(ctx, anchor, baseEnd, hover);
      renderSmallHandle(ctx, opposite, hover === 'OPPOSITE_HANDLE');
    }

  };

});
