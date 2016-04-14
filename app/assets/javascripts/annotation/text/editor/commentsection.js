define([], function() {

  var CommentSection = function(parent) {
    var element = jQuery(
      '<div class="section comments">' +
        '<textarea placeholder="Write a comment..." spellcheck="false" />' +
      '</div>');

    parent.append(element);
  };

  return CommentSection;

});
