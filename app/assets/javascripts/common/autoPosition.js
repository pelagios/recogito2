/**
 * Utility to sets the position and orientation of a popup element - such es the annotation
 * editor dialog - in response to available viewport space.
 */
define(function() {

  return {

    set : function(container, element, bounds) {
            var scrollTop = jQuery(document).scrollTop(),

                offset = jQuery(container).offset(),

                windowHeight = jQuery(window).height(),

                // Fixes bounds to take into account text container offset and scroll
                translatedBounds = {
                  bottom: bounds.bottom - offset.top + scrollTop,
                  height: bounds.height,
                  left: bounds.left - offset.left,
                  right: bounds.right - offset.left,
                  top: bounds.top - offset.top + scrollTop,
                  width: bounds.width
                },

                rectBefore, rectAfter;

            // Default orientation
            element.css({ top: translatedBounds.bottom, left: translatedBounds.left, bottom: 'auto' });
            rectBefore = element[0].getBoundingClientRect();

            // Flip horizontally, if popup exceeds screen width
            if (rectBefore.right > jQuery(window).width()) {
              element.addClass('align-right');
              element.css('left', translatedBounds.right - element.width());
            } else {
              element.removeClass('align-right');
            }

            // Flip vertically if popup exceeds screen height
            if (rectBefore.bottom > windowHeight) {
              element.addClass('align-bottom');
              element.css({ top: 'auto', bottom: container.clientHeight - translatedBounds.top });
            } else {
              element.removeClass('align-bottom');
            }

            // Still not visible? Scroll down
            rectAfter = element[0].getBoundingClientRect();
            if (rectAfter.bottom > windowHeight || rectAfter.top < 100) {
              jQuery(document.body).scrollTop(50 + scrollTop + rectAfter.bottom - windowHeight);
            }
          }

  };

});
