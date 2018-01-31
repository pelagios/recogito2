define([], function() {

  var SLIDE_DURATION = 100;

  var FilterPanel = function(parentEl, toggleButton) {

    var element = jQuery(
          '<div class="filterpanel">' +
            '<div class="filterpanel-head"></div>' +
            '<div class="filterpanel-body">' +
              '<ul class="legend">' +
              '</ul>' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        legend = element.find('ul.legend'),

        open = function() {
          element.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        close = function() {
          element.velocity('slideUp', { duration: SLIDE_DURATION });
        },

        toggle = function() {
          if (element.is(':visible')) close();
          else open();
        };

    toggleButton.click(toggle);

    this.open = open;
    this.close = close;
  };

  return FilterPanel;

});
