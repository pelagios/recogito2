define(['editor/placesection', 'editor/commentsection'], function(PlaceSection, CommentSection) {

  /** The main annotation editor popup **/
  var Editor = function() {

    var element = (function() {
          var el = jQuery(
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
            '</div>');

          jQuery(document.body).append(el);
          el.draggable({ handle: '.handle' });
          return el;
        })(),

        btnCancel = element.find('button.cancel'),
        btnOk = element.find('button.ok'),

        bodies = element.find('.bodies'),

        // Just for testing & styling
        dummyPlaceSection = new PlaceSection(bodies),

        commentSection = new CommentSection(bodies),

        show = function() {
          element.show();
        },

        hide = function() {
          element.hide();
        };

    // hide();


    // TODO just a dummy
    btnCancel.click(hide);
    btnOk.click(hide);

    this.show = show;
    this.hide = hide;
  };

  return Editor;

});
