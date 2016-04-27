define(['../../../../common/formatting'], function(Formatting) {

  var CommentSection = function(parent, commentBody) {
    var element = jQuery(
          '<div class="section comment">' +
            '<div class="comment-body">' + commentBody.value + '</div>' +
            '<div class="created">' +
              '<a class="by" href="/' + commentBody.last_modified_by + '">' +
                commentBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(commentBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>'),

          destroy = function() {
            element.remove();
          };

    parent.append(element);

    this.destroy = destroy;
  };

  return CommentSection;

});
