define([
  'common/config',
  'document/annotation/table/bulk/bulkEditorBase'
], function(Config, BaseBulkEditor) {

  var PlaceBulkEditor = function(columns) {
    var options =
          '<option></option>' +
          columns.map(function(name) {
            return '<option>' + name + '</option>';
          }).join(''),

        form = jQuery(
          '<form class="crud">' +
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

            '<button type="submit" class="btn">Go</button>' +
            '<button class="btn outline">Cancel</button>' +
          '</form>'),

        onSubmit = function() {
          var undefinedIfEmpty = function(str) {
                if (str.trim().length === 0)
                  return undefined;
                else
                  return str;
              },

              placeColumn = undefinedIfEmpty(form.find('#place-column select').val()),
              latColumn = undefinedIfEmpty(form.find('#lat-column select').val()),
              lonColumn = undefinedIfEmpty(form.find('#lon-column select').val());

          try {
            jsRoutes.controllers.api.TaskAPIController.spawnTask().ajax({
              data: JSON.stringify({
                task_type   : 'GEORESOLUTION',
                document_id : Config.documentId,
                filepart_id : Config.partId,
                args        : {
                  place_column : placeColumn,
                  lat_column   : latColumn,
                  lon_column   : lonColumn
                }
              }),
              contentType: 'application/json; charset=utf-8'
            }).success(function(response) {
              // TODO
              console.log(response);
            }).fail(function(error) {
              // TODO
              console.log(error);
            });
          } catch (error) {
            console.log(error);
          }
          return false;

        };

    form.submit(onSubmit);

    BaseBulkEditor.apply(this, [ 'Place Annotation', form ]);
  };
  PlaceBulkEditor.prototype = Object.create(BaseBulkEditor.prototype);

  return PlaceBulkEditor;

});
