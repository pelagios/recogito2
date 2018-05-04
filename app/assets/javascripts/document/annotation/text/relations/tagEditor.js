define(['common/hasEvents'], function(HasEvents) {

  // https://github.com/twitter/typeahead.js/issues/235
  var original = jQuery.fn.val;
  jQuery.fn.val = function() {
    if ($(this).is('*[contenteditable=true]'))
      return jQuery.fn.html.apply(this, arguments);
    return original.apply(this, arguments);
  };

  var TagEditor = function(containerEl, position) {
    var that = this,

        element = jQuery(
          '<div class="connection-editor-popup">' +
            '<span class="input" contentEditable="true" data-placeholder="Tag..."></span>' +
          '</div>').appendTo(containerEl),

        inputEl = element.find('span'),

        init = function() {
              // TODO autocomplete matcher
          var matcher = function(query, responseFn) {
                responseFn([]);
              };

          element.css({ top: position[1] - 15, left: position[0] });

          inputEl.keydown(onKeydown);
          inputEl.typeahead({ hint:false },{ source: matcher });

          setTimeout(function() { inputEl.focus(); }, 1);
        },

        onKeydown = function(e) {
          if (e.keyCode == 13) {
            that.fireEvent('submit');
            element.remove();
            return false;
          }
        };

    init();
    HasEvents.apply(this);
  };
  TagEditor.prototype = Object.create(HasEvents.prototype);

  return TagEditor;

});
