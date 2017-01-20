require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/modal',
  'common/config'
], function(Modal, Config) {

  var SettingsModal = function(fields) {
    var self = this,

        options =
          '<option></option>' + // Empty option
          fields.map(function(name, idx) {
            return '<option value="' + idx + '">' + name + '</option>';
          }).join(''),

        body = jQuery(
          '<div>' +
            '<form class="crud">' +
              '<p class="instructions">Configure the columns that correspond to gazetteer ' +
                'properties. Unmapped columns will be exported as generic GeoJSON properties.</p>' +

              // TODO base URL

              '<dl id="id">' +
                '<dt><label for="id">Unique ID</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl id="title">' +
                '<dt><label for="title">Title</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl class="name">' +
                '<dt><label>Name</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl class="add-name">' +
                '<dt></dt>' +
                '<dd><span class="icon">&#xf055;</span> ' +
                  '<span class="label">Add Name column</span></dd>' +
              '</dl>' +

              '<dl id="description">' +
                '<dt><label for="title">Description</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl id="country">' +
                '<dt><label for="country">Country Code</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
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
                '<button type="submit" class="btn">OK</button>' +
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

    Modal.apply(this, [ 'Gazetteer Export Settings', body, 'settings' ]);

    init();
    self.open();
  };
  SettingsModal.prototype = Object.create(Modal.prototype);

  jQuery(document).ready(function() {

        // We only need the header fields, but fetch first 5 so that
        // Papa Parse can do proper delimiter guessing
    var CSV_SNIPPET_SIZE = 5,

        // For lack of a better option, we pull header fields from the first part *only*,
        // It's in the responsibility of the user if they want deal with a mix of tables with
        // different schemas. Recogito will only support the base case.
        dataURL = jsRoutes.controllers.document.DocumentController
          .getDataTable(Config.documentId, Config.dataPartSequenceNo[0], CSV_SNIPPET_SIZE).absoluteURL(),

        btnSettings = jQuery('.settings'),

        fields = [], // to be populated from the CSV snippet

        init = function(csv) {
          fields = csv.meta.fields;
          btnSettings.click(openSettings);
        },

        openSettings = function() {
          new SettingsModal(fields);
          return false;
        };

    Papa.parse(dataURL, {
      download : true,
      header   : true, // TODO can we make this configurable through extra table meta?

      // TODO we're not doing an error handling at the moment, but just settings unpopulated
      complete : init
    });

  });

});
