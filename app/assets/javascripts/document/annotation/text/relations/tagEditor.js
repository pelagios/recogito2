define([
  'common/hasEvents',
  'document/annotation/text/relations/tagVocabulary'
], function(HasEvents, Vocabulary) {

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
          if (e.keyCode == 13) {
            var tag = inputEl.val();

            jQuery('.connection-editor-popup').remove();

            Vocabulary.add(tag);

            that.fireEvent('submit', tag);

            console.log('removing');
            console.log(element[0]);

            return false;
          }
        };

    init();
    HasEvents.apply(this);
  };
  TagEditor.prototype = Object.create(HasEvents.prototype);

  return TagEditor;

});
