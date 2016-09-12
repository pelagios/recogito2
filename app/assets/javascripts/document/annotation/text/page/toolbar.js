define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var Toolbar = function(rootNode) {
    var self = this,

        annotationModes = jQuery('.annotation-mode'),

        quickModeMenu = annotationModes.find('.submenu'),

        colorSchemes = jQuery('.color-scheme'),

        currentMode = { mode: 'NORMAL' },

        currentColorScheme = 'BY_TYPE',

        maxScroll = jQuery('.header-infobox').outerHeight() -
          jQuery('.header-iconbar').outerHeight() + 1,

        makeToolbarSticky = function() {
          var onScroll = function() {
            var scrollTop = jQuery(window).scrollTop();
            if (scrollTop > maxScroll) // Title area minus height of icon bar
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

        attachClickHandlers = function() {
          annotationModes.on('click', 'li', function(e) {
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
                // Submenu selection
                menuItemQuick.addClass('active ' + type);
                quickModeMenu.hide();
              } else {
                t.addClass('active');
              }
              currentMode = { mode: mode, type: type };
              self.fireEvent('annotationModeChanged', currentMode);
            }
          });

          colorSchemes.on('click', 'li', function(e) {
            var t = jQuery(e.target).closest('li'),
                active = t.hasClass('active'),
                scheme = t.data('scheme');

            if (!active) {
              colorSchemes.find('.active').removeClass('active');
              t.addClass('active');
              self.fireEvent('colorschemeChanged', scheme);
            }
          });
        },

        getCurrentAnnotationMode = function() {
          return currentMode;
        },

        setCurrentColorscheme = function(mode) {
          colorSchemes.find('li.active').removeClass('active');
          jQuery('[data-scheme="' + mode + '"]').addClass('active');
        };

    initQuickModeMenu();
    makeToolbarSticky();
    attachClickHandlers();

    this.setCurrentColorscheme = setCurrentColorscheme;
    this.getCurrentAnnotationMode = getCurrentAnnotationMode;

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
