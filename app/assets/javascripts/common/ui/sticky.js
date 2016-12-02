define(['common/config'], function(Config) {

  return {

    makeSticky : function(element, maxScroll) {
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
    }

  };

});
