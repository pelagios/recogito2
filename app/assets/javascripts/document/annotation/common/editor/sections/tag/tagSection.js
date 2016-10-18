define([
  'document/annotation/common/editor/sections/section'
], function(Section, TextEntryField) {

  var TagSection = function(parent, tagBodies) {
    var element = jQuery(
          '<div class="section tags">' +
            '<ul></ul>' +
            '<div contenteditable="true" spellcheck="false" class="add-tag" data-placeholder="Add tag..." />' +
          '</div>'),

        taglist = element.find('ul'),
        textarea = element.find('.add-tag'),

        changed = false,

        hasChanged = function() {
          return changed;
        },

        commit = function() {
          // TODO implement
        },

        destroy = function() {
          element.remove();
        },

        addTag = function(chars) {
          changed = true;
          taglist.append('<li>' + chars + '</li>');
        },

        onKeyDown = function(e) {
          if (e.keyCode === 13) {
            addTag(textarea.text().trim());
            textarea.empty();
            textarea.blur();
            return false;
          }
        };

    textarea.keydown(onKeyDown);
    parent.append(element);

    this.hasChanged = hasChanged;
    this.commit = commit;
    this.destroy = destroy;
    this.body = {}; // N/A

    Section.apply(this);
  };
  TagSection.prototype = Object.create(Section.prototype);

  return TagSection;

});
