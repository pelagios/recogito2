define([], function() {

  var LoadIndicator = function() {

    var element = jQuery(
          '<div class="load-clicktrap">' +
            '<div class="load-icon">' +
              '<img src="/assets/images/wait-circle-static.gif">' +
            '</div>' +
          '</div>'),

        init = function(containerEl) {
          var windowHeight = window.innerHeight,
              headerHeight = jQuery(containerEl).position().top,
              visibleContentHeight = windowHeight - headerHeight,
              loadIcon = element.find('.load-icon');

          loadIcon.css('padding-top', visibleContentHeight / 2 - 12);
          loadIcon.hide();

          jQuery(containerEl).append(element);
          loadIcon.fadeIn(250);
        },

        destroy = function() {
          element.fadeOut({
            duration: 150,
            complete: function() { element.remove(); }
          });
        };

    this.init = init;
    this.destroy = destroy;
  };

  return LoadIndicator;

});
