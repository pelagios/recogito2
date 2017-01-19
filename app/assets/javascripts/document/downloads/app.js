require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/modal'
], function(Modal) {

  var SettingsModal = function() {
    var self = this,

        body = jQuery(
          '<div>' +
            '<form class="crud">' +
              '<p class="instructions">Configure the spreadsheet columns that ' +
                'correspond to gazetteer properties. Unmapped columns will be exported as ' +
                'generic GeoJSON properties.</p>' +

              // TODO base URL

              '<dl id="id">' +
                '<dt><label for="id">ID</label></dt>' +
                '<dd><select>' + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl id="title">' +
                '<dt><label for="title">Title</label></dt>' +
                '<dd><select>' + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl class="name">' +
                '<dt><label>Name</label></dt>' +
                '<dd><select>' + '</select></dd>' +
              '</dl>' +

              '<dl class="add-name">' +
                '<dt></dt>' +
                '<dd><span class="icon">&#xf055;</span> ' +
                  '<span class="label">Add Name column</span></dd>' +
              '</dl>' +

              '<dl id="description">' +
                '<dt><label for="title">Description</label></dt>' +
                '<dd><select>' + '</select></dd>' +
              '</dl>' +

              '<dl id="country">' +
                '<dt><label for="country">Country Code</label></dt>' +
                '<dd><select>' + '</select></dd>' +
              '</dl>' +

              // TODO geometry
              
              /*
              '<dl class="lat">' +
                '<dt><label for="lat">Latitude</label></dt>' +
                '<dd><select>' + '</select></dd>' +
                '<dd class="info"></dd>' +
              '</dl>' +

              '<dl class="lon">' +
                '<dt><label for="lon">Longitude</label></dt>' +
                '<dd><select>' + '</select></dd>' +
                '<dd class="info"></dd>' +
              '</dl>' +
              */

              '<div class="buttons">' +
                '<button type="submit" class="btn">Go</button>' +
                '<button class="btn outline cancel">Cancel</button>' +
              '</div>' +
            '</form>' +
          '</div>'),

        init = function() {
          body.find('button.cancel').click(onCancel);
        },

        onCancel = function() {
          self.destroy();
          return false;
        };

    Modal.apply(this, [ 'Settings', body, 'settings' ]);

    init();
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
