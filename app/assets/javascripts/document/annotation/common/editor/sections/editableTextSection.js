/**
 * A base class that encapsulates the common features of comment and transcription sections.
 */
define([
  'common/config',
  'document/annotation/common/editor/sections/section'], function(Config, Section) {

  var EditableTextSection = function(element, annotationBody) {
    var self = this;

    this.element = element;
    this.body = annotationBody;

    this.textEntryDiv = element.find('.text');
    this.lastModified = element.find('.last-modified');

    this.textEntryDiv.html(self.escapeHtml(annotationBody.value));

    Section.apply(this);
  };
  EditableTextSection.prototype = Object.create(Section.prototype);

  /** TODO we may want to allow HTML later - but then need to sanitize **/
  EditableTextSection.prototype.escapeHtml = function(text) {
    return jQuery('<div/>').text(text).html();
  };

  // Cf. http://stackoverflow.com/questions/13513329/set-cursor-to-the-end-of-contenteditable-div
  EditableTextSection.prototype.placeCaretAtEnd = function(element) {
    var range = document.createRange(),
        selection = window.getSelection();

    range.setStart(element, 1);
    range.collapse(true);
    selection.removeAllRanges();
    selection.addRange(range);
    element.focus();
  };

  EditableTextSection.prototype.makeEditable = function() {
    var self = this;

    this.element.addClass('editable');
    this.textEntryDiv.prop('contenteditable', true);
    this.placeCaretAtEnd(this.textEntryDiv[0]);
    this.lastModified.remove();

    this.textEntryDiv.keyup(function(e) {
      if (e.ctrlKey && e.keyCode == 13)
        self.fireEvent('submit');
    });
  };

  EditableTextSection.prototype.hasChanged = function() {
    var initialContent = this.body.value,
        currentContent = this.textEntryDiv.text().trim();

    return initialContent !== currentContent;
  };

  EditableTextSection.prototype.commit = function() {
    if (this.hasChanged()) {
      delete this.body.last_modified_at;
      this.body.last_modified_by = Config.me;
      this.body.value = this.textEntryDiv.text().trim();
    }
  };

  EditableTextSection.prototype.destroy = function() {
    this.element.remove();
  };

  return EditableTextSection;

});
