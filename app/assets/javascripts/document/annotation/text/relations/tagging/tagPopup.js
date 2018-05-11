define([
  'common/hasEvents',
  'document/annotation/text/relations/tagging/tagVocabulary'
], function(HasEvents, Vocabulary) {

  // https://github.com/twitter/typeahead.js/issues/235
  var original = jQuery.fn.val;
  jQuery.fn.val = function() {
    if ($(this).is('*[contenteditable=true]'))
      return jQuery.fn.html.apply(this, arguments);
    return original.apply(this, arguments);
  };

  var TagPopup = function(containerEl, position) {
    var that = this,

        element = jQuery(
          '<div class="connection-editor-popup">' +
            '<div class="input" contentEditable="true" data-placeholder="Tag..."></div>' +
            '<div class="buttons">' +
              '<span class="icon delete">&#xf014;</span>' +
              '<span class="icon ok">&#xf00c;</span>' +
            '</div>' +
          '</div>').appendTo(containerEl),

        inputEl = element.find('.input'),

        init = function() {
          var matcher = function(query, responseFn) {
                var matches = [];

                Vocabulary.tags.forEach(function(tag) {
                  if (tag.toLowerCase().indexOf(query.toLowerCase()) === 0)
                    matches.push(tag);
                });

                responseFn(matches);
              };

          element.css({ top: position[1] - 15, left: position[0] });

          inputEl.keydown(onKeydown);
          inputEl.typeahead({ hint:false },{ source: matcher });

          setTimeout(function() { inputEl.focus(); }, 1);
        },

        onKeydown = function(e) {
          // Won't work with element due to Typeahead
          var el = jQuery('.connection-editor-popup');

          if (e.which === 27) { // Escape
            el.remove();
            that.fireEvent('cancel');
          } else if (e.which == 13) { // Enter
            var tag = inputEl.val();
            el.remove();

            Vocabulary.add(tag);

            that.fireEvent('submit', tag);
            return false;
          }
        };

    init();
    HasEvents.apply(this);
  };
  TagPopup.prototype = Object.create(HasEvents.prototype);

  return TagPopup;

});
