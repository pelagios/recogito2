define(['../../../../common/hasEvents'], function(HasEvents) {

  var CommentSection = function(parent) {
    var self = this,

        element = jQuery(
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

    textarea.keyup(function(e) {
      if (e.ctrlKey && e.keyCode == 13)
        self.fireEvent('submit', getComment());
    });

    parent.append(element);

    this.clear = clear;
    this.getComment = getComment;

    HasEvents.apply(this);
  };
  CommentSection.prototype = Object.create(HasEvents.prototype);

  return CommentSection;

});
