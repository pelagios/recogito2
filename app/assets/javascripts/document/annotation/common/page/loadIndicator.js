define([], function() {

  var LoadIndicator = function(containerEl) {
    var clicktrap = jQuery('.load-clicktrap'),

        init = function() {
          var windowHeight = window.innerHeight,

              headerHeight = jQuery(containerEl).position().top,

              visibleContentHeight = windowHeight - headerHeight,

              loadIcon = jQuery(
                '<div class="load-icon">' +
                  '<img src="/assets/images/wait-circle-static.gif">' +
                '</div>');

          loadIcon.css('padding-top', visibleContentHeight / 2 - 12);
          loadIcon.hide();

          clicktrap.append(loadIcon);
          loadIcon.fadeIn(250);
        },

        destroy = function() {
          clicktrap.fadeOut({
            duration: 150,
            complete: function() { clicktrap.remove(); }
          });
        };

    init();

    this.destroy = destroy;
  };

  return LoadIndicator;

});
