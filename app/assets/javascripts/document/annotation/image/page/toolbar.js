define(['common/hasEvents'], function(HasEvents) {

  var Toolbar = function() {
    var self = this,

        toolDropdown = jQuery('.dropdown'),

        initToolDropdown = function() {
          toolDropdown.hide();
          toolDropdown.parent().hover(
            function() { toolDropdown.show(); },
            function() { toolDropdown.hide(); });
        };

    initToolDropdown();

    HasEvents.apply(this);
  };
  Toolbar.prototype = Object.create(HasEvents.prototype);

  return Toolbar;

});
