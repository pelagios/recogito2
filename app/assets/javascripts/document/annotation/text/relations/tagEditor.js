define(['common/hasEvents'], function(HasEvents) {

  var TagEditor = function(containerEl, position) {
    var that = this,

        element = jQuery(
          '<div class="connection-editor-popup">' +
            '<input type="text" placeholder="Add tag..."></input>' +
          '</div>').appendTo(containerEl),

        inputEl = element.find('input'),

        init = function() {
          element.css({ top: position[1] - 15, left: position[0] });
          setTimeout(function() { inputEl.focus(); }, 1);
          inputEl.keyup(onKeyUp);
        },

        onKeyUp = function(e) {
          if (e.keyCode == 13)
            that.fireEvent('submit');
        };

    init();
    HasEvents.apply(this);
  };
  TagEditor.prototype = Object.create(HasEvents.prototype);

  return TagEditor;

});
