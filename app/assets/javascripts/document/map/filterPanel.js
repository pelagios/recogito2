define([], function() {

  var SLIDE_DURATION = 150;

  var FilterPanel = function(parentEl, toggleButton) {

    var element = jQuery(
          '<div class="filterpanel">' +
          '</div>').hide().appendTo(parentEl),

        open = function() {
          element.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        close = function() {
          element.velocity('slideUp', { duration: SLIDE_DURATION });
        },

        toggle = function() {
          if (element.is(':visible'))
            close();
          else
            open();
        };

    toggleButton.click(toggle);

    this.open = open;
    this.close = close;
  };

  return FilterPanel;

});
