define([
  'common/ui/modal',
  'common/config',
  'document/annotation/table/bulk/progressModal'
], function(Modal, Config, ProgressModal) {

  var PlaceBulkEditor = function(metadata) {

    var self = this,

        options =
          '<option></option>' +
          metadata.fields.map(function(name, idx) {
            return '<option value="' + idx + '">' + name + '</option>';
          }).join(''),

        gazetteers = // TODO
          '<option></option>',

        form = jQuery(
          '<form class="crud">' +
            '<p class="instructions">' +
              'To generate automatic gazetteer matches for your data, select a column below. ' +
              'The contents of this column will be matched against the built-in gazetteers.' +
            '</p>' +

            '<dl id="place-column">' +
              '<dt><label for="place-column">Placename</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
              '<dd class="info">Required</dd>' +
            '</dl>' +

            '<p class="instructions">' +
              'If your data includes geo-coordinates, you can use them to ' +
              'disambiguate the matches.' +
            '</p>' +

            '<dl id="lat-column">' +
              '<dt><label for="lat-column">Latitude</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
              '<dd class="info">Optional</dd>' +
            '</dl>' +

            '<dl id="lon-column">' +
              '<dt><label for="lon-column">Longitude</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
              '<dd class="info">Optional</dd>' +
            '</dl>' +

            '<p class="instructions">' +
              'Optionally, choose a specific gazetteer to be preferred, ' +
              'or used exclusively.' +
            '</p>' +

            '<dl id="preferred-gazetteer">' +
              '<dt><label for="preferred-gazetteer">Preferred gazetteer</label></dt>' +
              '<dd><select disabled="true">' + gazetteers + '</select></dd>' +
              '<dd class="info">Optional</dd>' +
            '</dl>' +

            '<dl id="use-exclusive">' +
              '<dt></dt>' +
              '<dd>' +
                '<input type="checkbox" id="exclusive" disabled="true">' +
                '<label for="exclusive">Exclusive</label>' +
              '</dd>' +
            '</dl>' +

            '<div class="buttons">' +
              '<button type="submit" class="btn">Go</button>' +
              '<button class="btn outline cancel">Cancel</button>' +
            '</div>' +
          '</form>'),

        btnCancel = form.find('button.cancel'),

        onSubmit = function() {
          var progressModal = new ProgressModal(Config.documentId),

              undefinedIfEmpty = function(str) {
                if (str.trim().length === 0)
                  return undefined;
                else
                  return str;
              },

              placeColumn = undefinedIfEmpty(form.find('#place-column select').val()),
              latColumn = undefinedIfEmpty(form.find('#lat-column select').val()),
              lonColumn = undefinedIfEmpty(form.find('#lon-column select').val()),

              onStopped = function(result) {
                progressModal.destroy();
                location.reload(true);
              };

          progressModal.on('stopped', onStopped);

          jsRoutes.controllers.api.TaskAPIController.spawnTask().ajax({
            data: JSON.stringify({
              task_type   : 'GEORESOLUTION',
              document_id : Config.documentId,
              filepart_id : Config.partId,
              args        : {
                delimiter    : metadata.delimiter,
                toponym_column : placeColumn,
                lat_column   : latColumn,
                lon_column   : lonColumn
              }
            }),
            contentType: 'application/json; charset=utf-8'
          }).success(function(response) {
            progressModal.open();
            self.destroy();
          }).fail(function(error) {
            // TODO error popup
            console.log(error);
          });

          return false;

        },

        onCancel = function() {
          self.destroy();
          return false;
        };

    form.submit(onSubmit);
    btnCancel.click(onCancel);

    Modal.apply(this, [ 'Automatic Gazetteer Resolution', form, 'wizard' ]);

    self.open();
  };
  PlaceBulkEditor.prototype = Object.create(Modal.prototype);

  return PlaceBulkEditor;

});
