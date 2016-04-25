define([], function() {

  var CommentSection = function(parent) {
    var element = jQuery(
          '<div class="section comments">' +
            '<div contenteditable="true" spellcheck="false" class="textarea" data-placeholder="Add a comment..." />' +
          '</div>'),

          textarea = element.find('.textarea'),

          clear = function() {
            textarea.empty();
          },

          getComment = function() {
            var val = textarea.text().trim();
            if (val)
              return { type: 'COMMENT', value: val };
            else
              return false;
          };

    parent.append(element);

    this.clear = clear;
    this.getComment = getComment;
  };

  return CommentSection;

});
