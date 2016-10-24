define([], function() {

  var LoadIndicator = function(containerEl) {
    var clickTrap = jQuery('.load-clicktrap'),

        init = function() {
          var windowHeight = window.innerHeight,

              headerHeight = jQuery(containerEl).position().top,

              visibleContentHeight = windowHeight - headerHeight,

              loadIcon = jQuery(
                '<div class="load-icon">' +
                  '<img src="/assets/images/wait-circle-static.gif">' +
                '</div>'),

              showIcon = function() {
                clickTrap.append(loadIcon);
                loadIcon.fadeIn(250);
              };

          loadIcon.css('padding-top', visibleContentHeight / 2 - 12);
          loadIcon.hide();

          // Only show icon if wait time exceeds 250ms
          setTimeout(showIcon, 250);
        },

        destroy = function() {
          // clickTrap.fadeOut(150);
        };

    init();

    this.destroy = destroy;
  };

  return LoadIndicator;

});
