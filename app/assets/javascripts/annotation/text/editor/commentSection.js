define([], function() {

  var CommentSection = function(parent) {
    var element = jQuery(
          '<div class="section comments">' +
            '<div contenteditable="true" spellcheck="false" class="textarea" data-placeholder="Add a comment..." />' +
          '</div>'),

          textArea = element.find('textarea'),

          /** Resizes the comment box so that it fits the whole text without scrollbar **/
          fitVertical = function() {
            textArea.css('height', 'auto');
            textArea.height(textArea[0].scrollHeight);
          };

    textArea.keyup(fitVertical);
    parent.append(element);
  };

  return CommentSection;

});
