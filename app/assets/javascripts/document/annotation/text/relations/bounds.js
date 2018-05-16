define([], function() {

  var toUnionBoundingRects = function(elements) {
        var allRects = elements.toArray().reduce(function(arr, el) {
              var rectList = el.getClientRects(), // Note array-like, but *not* an array (sigh)
                  len = rectList.length, i;

              for (i = 0; i < len; i++) {
                arr.push(rectList[i]);
              }

              return arr;
            }, []);

        // TODO merge

        return allRects;
      },

      toOffsetBounds = function(clientBounds, offsetContainer) {
        var offset = offsetContainer.offset(),
            left = Math.round(clientBounds.left - offset.left),
            top = Math.round(clientBounds.top - offset.top + jQuery(window).scrollTop());

        return {
          left  : left,
          top   : top,
          right : Math.round(left + clientBounds.width),
          bottom: Math.round(top + clientBounds.height),
          width : Math.round(clientBounds.width),
          height: Math.round(clientBounds.height)
        };
      };

  var Bounds = function(elements, offsetContainer) {

    var offsetBounds = toUnionBoundingRects(elements).map(function(clientBounds) {
          return toOffsetBounds(clientBounds, offsetContainer);
        }),

        getTopHandleXY = function() {
          return [
            offsetBounds[0].left + offsetBounds[0].width / 2 + 0.5,
            offsetBounds[0].top
          ];
        },

        getBottomHandleXY = function() {
          var i = offsetBounds.length - 1;
          return [
            offsetBounds[i].left + offsetBounds[i].width / 2 - 0.5,
            offsetBounds[i].bottom
          ];
        };

    this.rects = offsetBounds;
    this.top = offsetBounds[0].top;
    this.bottom = offsetBounds[offsetBounds.length - 1].bottom;
    this.height = this.bottom - this.top;
    this.getTopHandleXY = getTopHandleXY;
    this.getBottomHandleXY = getBottomHandleXY;
  };

  return Bounds;

});
