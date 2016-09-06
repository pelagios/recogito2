define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var ICONS = {
    POINT     : '&#xf05b',
    RECTANGLE : '<span class="rect"></span>',
    TBOX   : '<span class="tilted-box"></span>'
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

        setTool = function(toolLabel, toolKey) {
          tools.find('.active').removeClass('active');

          if (toolKey === 'move') {
            tools.find('[data-tool-key="move"]').addClass('active');
          } else {
            // Submenu selection
            toolMenu.addClass('active');
            toolMenu.data('tool-label', toolLabel);
            toolMenu.data('tool-key', toolKey);
            toolMenuIcon.html(ICONS[toolKey]);
            toolMenuLabel.html(toolLabel);
          }

          imagePane.attr('class', toolKey);
          currentTool = toolKey;

          self.fireEvent('toolChanged', toolKey);
        },

        attachClickHandlers = function() {
          var attachToolHandler = function() {
                tools.on('click', 'li', function(e) {
                  var item = jQuery(e.target).closest('li'),
                      toolLabel = item.data('tool-label'),
                      toolKey = item.data('tool-key');

                  if (item.hasClass('disabled'))
                    return;

                  if (toolKey !== currentTool)
                    setTool(toolLabel, toolKey);

                  return false; // Prevent events from parent LIs
                });
              },

              attachHelpHandler = function() {
                jQuery('.help').click(toggleHelp);
              };

          attachToolHandler();
          attachHelpHandler();
        },

        toggleHelp = function() {
          self.fireEvent('toggleHelp');
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

          if (key === 32) { // SPACE - toggle MOVE/annotation tool
            if (Config.writeAccess)
              toggleTool();
          } else if (key == 112) { // F1
              toggleHelp();
              return false; // To prevent browser help from popping up
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
