define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var ICONS = {
    POINT     : '&#xf05b',
    RECTANGLE : '<span class="rect"></span>',
    TOPONYM   : '<span class="toponym"></span>'
  };

  var Toolbar = function() {
    var self = this,

        tools = jQuery('.tools'),

        toolMenu = tools.find('.has-submenu'),
        toolMenuIcon = tools.find('.has-submenu > .icon'),
        toolMenuLabel = tools.find('.has-submenu > .label'),
        toolMenuDropdown = toolMenu.find('.submenu'),

        currentTool = 'MOVE',

        imagePane = jQuery('#image-pane'),

        initToolDropdown = function() {
          if (Config.writeAccess) {
              toolMenuDropdown.hide();
              toolMenu.hover(
                function() { toolMenuDropdown.show(); },
                function() { toolMenuDropdown.hide(); });
          } else {
            // Read-only mode
            toolMenu.addClass('disabled');
          }
        },

        setTool = function(toolName) {
          tools.find('.active').removeClass('active');

          if (toolName === 'MOVE') {
            tools.find('[data-tool="MOVE"]').addClass('active');
          } else {
            // Submenu selection
            toolMenu.addClass('active');
            toolMenu.data('tool', toolName);
            toolMenuIcon.html(ICONS[toolName]);
            toolMenuLabel.html(toolName);
          }

          imagePane.attr('class', toolName);
          currentTool = toolName;

          self.fireEvent('toolChanged', toolName);
        },

        attachClickHandlers = function() {
          var attachToolHandler = function() {
                tools.on('click', 'li', function(e) {
                  var item = jQuery(e.target).closest('li'),
                      tool = item.data('tool');

                  if (item.hasClass('disabled'))
                    return;

                  if (tool !== currentTool)
                    setTool(tool);

                  return false; // Prevent events from parent LIs
                });
              },

              attachHelpHandler = function() {
                jQuery('.help').click(function() {
                  self.fireEvent('toggleHelp');
                });
              };

          attachToolHandler();
          attachHelpHandler();
        },

        toggleTool = function() {
          var activeMode = tools.find('.active').data('tool'),
              selectedTool = toolMenu.data('tool');

          if (activeMode === 'MOVE')
            setTool(selectedTool);
          else
            setTool('MOVE');
        },

        onKeyDown = function(e) {
          var key = e.which;
          if (key === 32) {
            // SPACE - toggle MOVE/annotation tool
            toggleTool();
          }
        };

    initToolDropdown();
    attachClickHandlers();

    jQuery(window).keydown(onKeyDown);

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
