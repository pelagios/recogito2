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

        form = jQuery(
          '<form class="crud">' +
            // '<p class="caption"></p>' +
            '<dl id="place-column">' +
              '<dt><label for="place-column">Place name column</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
            '</dl>' +

            '<dl id="lat-column">' +
              '<dt><label for="lat-column">Latitude column</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
            '</dl>' +

            '<dl id="lon-column">' +
              '<dt><label for="lon-column">Longitude column</label></dt>' +
              '<dd><select>' + options + '</select></dd>' +
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

    Modal.apply(this, [ 'Place Annotation', form, 'wizard' ]);

    self.open();
  };
  PlaceBulkEditor.prototype = Object.create(Modal.prototype);

  return PlaceBulkEditor;

});
