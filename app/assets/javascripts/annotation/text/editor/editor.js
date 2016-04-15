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
              '<button class="btn small outline cancel">Cancel</button>' +
              '<button class="btn small ok">OK</button>' +
            '</div>' +
          '</div>'),

        btnCancel = element.find('button.cancel'),
        btnOk = element.find('button.ok'),

        commentSection = new CommentSection(element.find('.bodies')),


        show = function() {
          element.show();
        },

        hide = function() {
          element.hide();
        };

    // hide();
    jQuery(document.body).append(element);
    element.draggable({ handle: '.handle' });

    // TODO just a dummy
    btnCancel.click(hide);
    btnOk.click(hide);

    this.show = show;
    this.hide = hide;
  };

  return Editor;

});
