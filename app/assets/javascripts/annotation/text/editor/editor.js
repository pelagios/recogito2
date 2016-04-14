define([], function() {

  /** The main annotation editor popup **/
  var Editor = function() {

    var element = jQuery(
          '<div class="text-annotation-editor">' +
            '<div class="handle"></div>' +
            '<div class="category-buttons">' +
              '<div class="category-place">' +
              '<div class="category-person">' +
              '<div class="category-event">' +
            '</div>' +
            '<div class="bodies">' +
            '</div>' +
            '<div class="footer">' +
            '</div>' +
          '</div>');

  };

  return Editor;

});
