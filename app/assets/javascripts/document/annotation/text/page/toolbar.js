define([
  'common/ui/behavior',
  'common/config',
  'common/hasEvents'
], function(Behavior, Config, HasEvents) {

  var Toolbar = function(rootNode) {
    var self = this,

        annotationModes = jQuery('.annotation-mode'),

        quickModeMenu = annotationModes.find('.submenu'),

        colorSchemes = jQuery('.color-scheme'),

        currentMode = { mode: 'NORMAL' },

        currentColorScheme = 'BY_TYPE',

        maxScroll = jQuery('.header-infobox').outerHeight() -
          jQuery('.header-iconbar').outerHeight() + 1,

        disableAnnotationControls = function() {
          var menuItems = annotationModes.find('li');
          menuItems.addClass('disabled');
          menuItems.removeClass('active');
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

            // TODO temporary: disable TAGS quick mode
            if (type === 'TAGS')
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

            return false;
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

    if (Config.writeAccess)
      initQuickModeMenu();
    else
      disableAnnotationControls();

    Behavior.makeElementSticky(rootNode, maxScroll);
    attachClickHandlers();

    this.setCurrentColorscheme = setCurrentColorscheme;
    this.getCurrentAnnotationMode = getCurrentAnnotationMode;

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
