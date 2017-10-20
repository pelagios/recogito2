define([
  'document/annotation/common/editor/sections/editableTextSection',
  'common/ui/formatting',
  'common/config'
], function(EditableTextSection, Formatting, Config) {

  var TranscriptionSection = function(parent, transcriptionBody) {
    var self = this,

        element = jQuery(
          '<div class="section editable-text transcription">' +
            '<div class="text"></div>' +
            '<div class="icon edit">&#xf0dd;</div>' +
            '<div class="last-modified">' +
              '<a class="by" href="/' + transcriptionBody.last_modified_by + '">' +
                transcriptionBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(transcriptionBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>'),

        activateAddNoteFeature = function() {
          var btnAddNote = jQuery('<span class="icon add-note">&#xf249;</span>').appendTo(element);
          // Just a temporary hack to test this feature
          btnAddNote.click(function() {
            var note = prompt('Add note:', transcriptionBody.note);
            if (note) {
              self.changedNote = note;
            }
          });
        };

    if (Config.hasFeature('multitranscription'))
      activateAddNoteFeature();

    this.changedNote = false;

    parent.append(element);
    EditableTextSection.apply(this, [ element, transcriptionBody ]);
  };
  TranscriptionSection.prototype = Object.create(EditableTextSection.prototype);

  TranscriptionSection.prototype.hasChanged = function() {
    var hasSuperChanged = EditableTextSection.prototype.hasChanged.call(this);
    return hasSuperChanged || this.changedNote;
  };

  TranscriptionSection.prototype.commit = function() {
    if (this.changedNote) this.body.note = this.changedNote;
    EditableTextSection.prototype.commit.call(this);
  };

  return TranscriptionSection;

});
