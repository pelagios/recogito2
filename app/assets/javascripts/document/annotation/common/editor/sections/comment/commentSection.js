define([
  'document/annotation/common/editor/sections/editableTextSection',
  'common/ui/formatting',
  'common/config'], function(EditableTextSection, Formatting) {

  var CommentSection = function(parent, commentBody) {
    var element = jQuery(
          '<div class="section editable-text comment">' +
            '<div class="text"></div>' +
            '<div class="icon edit">&#xf0dd;</div>' +
            '<div class="last-modified">' +
              '<a class="by" href="/' + commentBody.last_modified_by + '">' +
                commentBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(commentBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>');

    parent.append(element);
    
    EditableTextSection.apply(this, [ element, commentBody ]);
  };
  CommentSection.prototype = Object.create(EditableTextSection.prototype);

  return CommentSection;

});
