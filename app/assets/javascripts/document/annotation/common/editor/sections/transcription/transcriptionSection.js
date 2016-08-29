define([
  'document/annotation/common/editor/sections/editableTextSection',
  'common/ui/formatting',
  'common/config'], function(EditableTextSection, Formatting, Config) {

  var TranscriptionSection = function(parent, transcriptionBody) {
    var self = this,

        element = jQuery(
          '<div class="section editable-text transcription">' +
            '<div class="text"></div>' +
            '<div class="icon delete">&#xf142;</div>' +
            '<div class="icon edit">&#xf142;</div>' +
            '<div class="icon add">&#xf142;</div>' +
            '<div class="last-modified">' +
              '<a class="by" href="/' + transcriptionBody.last_modified_by + '">' +
                transcriptionBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(transcriptionBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>'),

          btnDelete = element.find('.delete'),
          btnEdit = element.find('.edit'),
          btnAdd = element.find('.add'),

          hideButtons = function() {
            btnDelete.hide();
            btnEdit.hide();
            btnAdd.hide();
          };

    hideButtons();
    parent.append(element);

    EditableTextSection.apply(this, [ element, transcriptionBody ]);
  };
  TranscriptionSection.prototype = Object.create(EditableTextSection.prototype);

  return TranscriptionSection;

});
