define(['editor/commentsection'], function(CommentSection) {

  /** The main annotation editor popup **/
  var Editor = function() {

    var element = jQuery(
          '<div class="text-annotation-editor">' +
            '<div class="handle"></div>' +
            '<div class="category-buttons">' +
              '<div class="category place">' +
                '<span class="icon">&#xf041;</span> Mark as Place' +
              '</div>' +
              '<div class="category person">' +
                '<span class="icon">&#xf007;</span> Mark as Person' +
              '</div>' +
              '<div class="category event">' +
                '<span class="icon">&#xf005;</span> Mark as Event' +
              '</div>' +
            '</div>' +
            '<div class="bodies"></div>' +
            '<div class="footer">' +
              '<button class="btn small outline">Cancel</button>' +
              '<button class="btn small">OK</button>' +
            '</div>' +
          '</div>'),

        commentSection = new CommentSection(element.find('.bodies')),

        makeDraggable = function() {
          // TODO will become more elaborate later
          element.draggable({ handle: '.handle' });
        };

    jQuery(document.body).append(element);
    makeDraggable();

  };

  return Editor;

});
