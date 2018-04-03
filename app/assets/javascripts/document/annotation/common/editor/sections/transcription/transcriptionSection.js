define([
  'document/annotation/common/editor/sections/editableTextSection',
  'common/ui/formatting',
  'common/config'
], function(EditableTextSection, Formatting, Config) {

  var TranscriptionSection = function(parent, transcriptionBody) {

    var self = this,

        editNote = function() {
          var note = prompt('Add note:', transcriptionBody.note);
          if (note !== null) {
            if (note) // Non-empty string
              self.changedNote = { value: note };
            else
              self.changedNote = { value: false };
          }
          showNote();
        },

        addNoteMenuItem = [
          { label: 'Note', fn: editNote }
        ],

        element = jQuery(
          '<div class="section editable-text transcription">' +
            '<div class="text"></div>' +
            '<div class="note"></div>' +
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

        appendToParent = function() {
          var transcriptionsBefore = parent.find('.section.transcription'),
              idxToInsert = transcriptionsBefore.length;

          if (idxToInsert === 0)
            parent.prepend(element);
          else
            transcriptionsBefore.eq(idxToInsert - 1).after(element);
        },

        noteEl = element.find('.note'),

        showNote = function() {
          var shown = (self.changedNote) ? self.changedNote.value : transcriptionBody.note;
          if (shown) {
            noteEl.html(shown);
            noteEl.css('display', 'inline');
          } else {
            noteEl.html();
            hideNote();
          }
        },

        hideNote = function() {
          noteEl.css('display', 'none');
        };

    // Either false (no change) or { value: 'value' || false } for new/deleted note
    this.changedNote = false;

    appendToParent();

    if (Config.hasFeature('multitranscription')) {
      noteEl.click(editNote);
      showNote();
      EditableTextSection.apply(this, [ element, transcriptionBody, addNoteMenuItem ]);
    } else {
      EditableTextSection.apply(this, [ element, transcriptionBody ]);
    }
  };
  TranscriptionSection.prototype = Object.create(EditableTextSection.prototype);

  TranscriptionSection.prototype.hasChanged = function() {
    var hasSuperChanged = EditableTextSection.prototype.hasChanged.call(this);
    return hasSuperChanged || this.changedNote;
  };

  TranscriptionSection.prototype.commit = function() {
    if (this.changedNote)
      if (this.changedNote.value)
        this.body.note = this.changedNote.value;
      else
        delete this.body.note;

    EditableTextSection.prototype.commit.call(this);
  };

  return TranscriptionSection;

});
