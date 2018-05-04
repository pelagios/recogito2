define(['common/hasEvents'], function(HasEvents) {

  var TagEditor = function(containerEl, position) {
    var that = this,

        element = jQuery(
          '<div class="connection-editor-popup">' +
            '<span contentEditable="true" data-placeholder="Add tag..."></span>' +
          '</div>').appendTo(containerEl),

        inputEl = element.find('span'),

        init = function() {
          element.css({ top: position[1] - 15, left: position[0] });
          setTimeout(function() { inputEl.focus(); }, 1);
          inputEl.keydown(onKeydown);
        },

        onKeydown = function(e) {
          if (e.keyCode == 13) {
            that.fireEvent('submit');
            return false;
          }
        };

    init();
    HasEvents.apply(this);
  };
  TagEditor.prototype = Object.create(HasEvents.prototype);

  return TagEditor;

});
