require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/modal'
], function(Modal) {

  var SettingsModal = function() {
    var self = this,

        body = jQuery('<div></div>');

    Modal.apply(this, [ 'Settings', body, 'settings' ]);

    self.open();
  };
  SettingsModal.prototype = Object.create(Modal.prototype);

  jQuery(document).ready(function() {

    var btnSettings = jQuery('.settings'),

        openSettings = function() {
          new SettingsModal();
          return false;
        };

    btnSettings.click(openSettings);

  });

});
