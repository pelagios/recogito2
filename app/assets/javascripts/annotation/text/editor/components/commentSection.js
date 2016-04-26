define([], function() {

  var CommentSection = function(parent, commentBody) {
    var element = jQuery(
          '<div class="section comment">' +
            '<div>' + commentBody.value + '</div>' +
            '<div class="created">' +
              '<a class="by" href="/' + commentBody.last_modified_by + '">' +
                commentBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                jQuery.timeago(commentBody.last_modified_at) +
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
