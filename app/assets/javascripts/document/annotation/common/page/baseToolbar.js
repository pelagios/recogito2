define([
  'common/ui/behavior',
  'common/config',
  'common/hasEvents'
], function(Behavior, Config, HasEvents) {

  var Toolbar = function(rootNode) {
    var colorschemeButtons = jQuery('.color-scheme'),

        colorschemeStylesheet = jQuery('#colorscheme'),

        initDropdownMenus = function() {
          var submenus = jQuery('.submenu');
          submenus.hide();
          submenus.parent().hover(
            function() { jQuery(this).find('.submenu').show(); },
            function() { jQuery(this).find('.submenu').hide();  });
        },

        initColorscheme = function() {
          var storedColorscheme = localStorage.getItem('r2.document.edit.colorscheme'),
              colorscheme = (storedColorscheme) ? storedColorscheme : 'BY_STATUS';

          setCurrentColorscheme(colorscheme);

          colorschemeButtons.on('click', 'li', function(e) {
            var t = jQuery(e.target).closest('li'),
                active = t.hasClass('active'),
                scheme = t.data('scheme');

            if (!active) {
              colorschemeButtons.find('.active').removeClass('active');
              t.addClass('active');
              localStorage.setItem('r2.document.edit.colorscheme', scheme);
              setCurrentColorscheme(scheme);
            }
          });
        },

        setCurrentColorscheme = function(scheme) {
          var currentCSSPath = colorschemeStylesheet.attr('href'),
              basePath = currentCSSPath.substr(0, currentCSSPath.lastIndexOf('/'));

          if (scheme === 'BY_TYPE')
            colorschemeStylesheet.attr('href', basePath + '/colorByType.css');
          else
            colorschemeStylesheet.attr('href', basePath + '/colorByStatus.css');

          colorschemeButtons.find('li.active').removeClass('active');
          jQuery('[data-scheme="' + scheme + '"]').addClass('active');
        };

    initColorscheme();

    if (Config.writeAccess)
      initDropdownMenus();

    this.setCurrentColorscheme = setCurrentColorscheme;

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
