define(['common/config'], function(Config) {

  return {

    makeElementSticky : function(element, maxScroll) {
      var onScroll = function() {
        var scrollTop = jQuery(window).scrollTop();
        if (scrollTop > maxScroll)
          element.addClass('fixed');
        else
          element.removeClass('fixed');
      };

      if (Config.IS_TOUCH) {
        element.addClass('sticky');
      } else {
        // In case the page is initally scrolled after load
        onScroll();
        jQuery(window).scroll(onScroll);
      }
    },

    animateAnchorNav : function(containerEl) {
      containerEl.on('click', 'a', function(e) {
        e.preventDefault();

        jQuery('html, body').animate({
          scrollTop: jQuery(jQuery.attr(this, 'href')).offset().top
        }, 500);

        return false;
      });
    },

    placeCaretAtEnd : function(element) {
      var range = document.createRange(),
          selection = window.getSelection();

      range.setStart(element, 1);
      range.collapse(true);
      selection.removeAllRanges();
      selection.addRange(range);
      element.focus();
    }

  };

});
