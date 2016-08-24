define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var Toolbar = function() {
    var self = this,

        tools = jQuery('.tools'),

        toolMenu = tools.find('.has-submenu'),
        toolMenuIcon = toolMenu.find('.icon'),
        toolMenuLabel = toolMenu.find('.label'),
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
            tools.find('.has-submenu').addClass('active');
            toolMenuLabel.html(toolName);
          }

          imagePane.attr('class', toolName);
          currentTool = toolName;
        },

        attachClickHandlers = function() {
          tools.on('click', 'li', function(e) {
            var item = jQuery(e.target).closest('li'),
                tool = item.data('tool');

            // TODO temporary: disable RECTANGLE & TOPONYM tools
            if (tool === 'RECTANGLE' || tool === 'TOPONYM')
              return;

            if (tool && tool !== currentTool) {
              setTool(tool);
              self.fireEvent('toolChanged', tool);
            }

            return false; // Prevent events from parent LIs
          });
        };

    initToolDropdown();
    attachClickHandlers();

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
