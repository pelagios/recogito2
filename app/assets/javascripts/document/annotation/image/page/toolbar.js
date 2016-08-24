define(['common/hasEvents'], function(HasEvents) {

  var Toolbar = function() {
    var self = this,

        tools = jQuery('.tools'),

        toolDropdown = tools.find('.submenu'),
        toolDropdownIcon = tools.find('.has-submenu .icon'),
        toolDropdownLabel = tools.find('.has-submenu .label'),

        currentTool = 'MOVE',

        imagePane = jQuery('#image-pane'),

        initToolDropdown = function() {
          toolDropdown.hide();
          toolDropdown.parent().hover(
            function() { toolDropdown.show(); },
            function() { toolDropdown.hide(); });
        },

        setTool = function(toolName) {
          tools.find('.active').removeClass('active');

          if (toolName === 'MOVE') {
            tools.find('[data-tool="MOVE"]').addClass('active');
          } else {
            // Submenu selection
            tools.find('.has-submenu').addClass('active');
            toolDropdownLabel.html(toolName);
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
