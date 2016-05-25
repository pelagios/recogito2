define(['../../common/config', '../../common/hasEvents'], function(Config, HasEvents) {

  var Toolbar = function(rootNode) {
    var self = this,

        annotationModes = jQuery('.annotation-mode'),

        quickModeMenu = annotationModes.find('.quick .dropdown'),

        colorSchemes = jQuery('.color-scheme'),

        currentMode = { mode: 'NORMAL' },

        currentColorScheme = 'BY_TYPE',

        initQuickModeMenu = function() {
          quickModeMenu.hide();
          quickModeMenu.parent().hover(
            function() { quickModeMenu.show(); },
            function() { quickModeMenu.hide(); });
        },

        isModeChanged = function(mode, type)  {
          if (mode)
            return !(mode == currentMode.mode && type === currentMode.type);
          else
            // mode undefined: click on 'QUICK' header item - ignore
            return false;
        },

        attachButtonHandlers = function() {
          annotationModes.on('click', '>li', function(e) {
            var menuItemQuick = annotationModes.find('.quick'),
                t = jQuery(e.target).closest('li'),
                mode = t.data('mode'),
                type = t.data('type');

            // TODO temporary: disable PERSON & TAGS quick modes for 1st pre-release
            if (type === 'PERSON' || type === 'TAGS')
              return;

            if (isModeChanged(mode, type)) {
              annotationModes.find('.active').removeClass('active');
              menuItemQuick.removeClass(currentMode.type);
              if (type) {
                // Sub-menu selection
                menuItemQuick.addClass('active ' + type);
                quickModeMenu.hide();
              } else {
                t.addClass('active');
              }
              currentMode = { mode: mode, type: type };
              self.fireEvent('annotationModeChanged', currentMode);
            }
          });

          colorSchemes.on('click', '>li', function(e) {
            var t = jQuery(e.target).closest('li'),
                active = t.hasClass('active'),
                scheme = t.data('scheme');

            if (!active) {
              colorSchemes.find('.active').removeClass('active');
              t.addClass('active');
              self.fireEvent('colorModeChange', scheme);
            }
          });
        },

        makeToolbarSticky = function() {
          var onScroll = function() {
            var scrollTop = jQuery(window).scrollTop();
            if (scrollTop > 147)
              rootNode.addClass('fixed');
            else
              rootNode.removeClass('fixed');
          };

          if (Config.IS_TOUCH) {
            rootNode.addClass('sticky');
          } else {
            // In case the page is initally scrolled after load
            onScroll();
            jQuery(window).scroll(onScroll);
          }
        },

        getCurrentMode = function() {
          return currentMode;
        };

    initQuickModeMenu();
    makeToolbarSticky();
    attachButtonHandlers();

    this.getCurrentMode = getCurrentMode;

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
