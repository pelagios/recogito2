define([], function() {

      /** Helper to merge two bounds that have the same height + are exactly consecutive **/
  var mergeBounds = function(clientBounds) {
        if (clientBounds.length == 1) return clientBounds; // shortcut

        return clientBounds.reduce(function(merged, bbox) {
          var previous = (merged.length > 0) ? merged[merged.length - 1] : undefined,

              isConsecutive = function(a, b) {
                if (a.height === b.height)
                  return (a.x + a.width === b.x || b.x + b.width === a.x);
                else
                  return false;
              },

              extend = function(a, b) {
                a.x = Math.min(a.x, b.x);
                a.left = Math.min(a.left, b.left);
                a.width = a.width + b.width;
                a.right = Math.max(a.right + b.right);
              };

          if (previous) {
            if (isConsecutive(previous, bbox))
              extend(previous, bbox);
            else
              merged.push(bbox);
          } else {
            merged.push(bbox);
          }

          return merged;
        }, []);
      },

      /** Returns a clean list of (merged) DOMRect bounds for the given elements **/
      toUnionBoundingRects = function(elements) {
        var allRects = elements.toArray().reduce(function(arr, el) {
              var rectList = el.getClientRects(), // Note array-like, but *not* an array (sigh)
                  len = rectList.length, i;

              for (i = 0; i < len; i++) {
                arr.push(rectList[i]);
              }

              return arr;
            }, []);

        return mergeBounds(allRects);
      },

      /** Translates DOMRect client bounds to offset bounds within the given container **/
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

    var offsetBounds,

        recompute = function() {
          offsetBounds = toUnionBoundingRects(elements).map(function(clientBounds) {
            return toOffsetBounds(clientBounds, offsetContainer);
          });
        },

        getRects = function() {
          return offsetBounds;
        },

        getTop = function() {
          return offsetBounds[0].top;
        },

        getBottom = function() {
          return offsetBounds[offsetBounds.length - 1].bottom;
        },

        getHeight = function() {
          return getBottom() - getTop();
        },

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

    recompute();

    this.recompute = recompute;
    this.getRects = getRects;
    this.getTop = getTop;
    this.getBottom = getBottom;
    this.getHeight = getHeight;
    this.getTopHandleXY = getTopHandleXY;
    this.getBottomHandleXY = getBottomHandleXY;
  };

  return Bounds;

});
