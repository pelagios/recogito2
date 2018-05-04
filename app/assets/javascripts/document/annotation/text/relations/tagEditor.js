define([], function() {

  var TagEditor = function(containerEl, position) {
    var element = jQuery(
      '<div class="connection-editor-popup">Add tag...</div>').appendTo(containerEl);

    element.css({ top: position[1] - 15, left: position[0] });
  };

  return TagEditor;

});
