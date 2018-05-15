define([
  'common/hasEvents',
  'common/ui/behavior',
  'document/annotation/text/relations/tagging/tagVocabulary'
], function(HasEvents, Behavior, Vocabulary) {

  /**
   * A hack that patches jQuery, so that contentEditable elements work with typeahead like
   * normal inputs.
   *
   * https://github.com/twitter/typeahead.js/issues/235
   */
  var original = jQuery.fn.val;
  jQuery.fn.val = function() {
    if ($(this).is('*[contenteditable=true]'))
      return jQuery.fn.html.apply(this, arguments);
    return original.apply(this, arguments);
  };

  var TagPopup = function(containerEl, position, opt_tag) {

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

        btnDelete = element.find('.delete'),
        btnOk = element.find('.ok'),

        init = function() {

          // Trivial prefix-based autocomplete matcher
          var matcher = function(query, responseFn) {
                var matches = [];

                Vocabulary.tags.forEach(function(tag) {
                  if (tag.toLowerCase().indexOf(query.toLowerCase()) === 0)
                    matches.push(tag);
                });

                responseFn(matches);
              };

          element.css({ top: position[1] - 15, left: position[0] });

          if (opt_tag) inputEl.html(opt_tag);

          inputEl.keydown(onKeydown);
          inputEl.typeahead({ hint:false },{ source: matcher });

          btnDelete.click(onDelete);
          btnOk.click(onSubmit);

          setTimeout(function() {
            if (opt_tag) Behavior.placeCaretAtEnd(inputEl[0]);
            else inputEl.focus();
          }, 1);
        },

        onSubmit = function() {
          var tag = inputEl.val(),
              isEmpty = (tag) ? tag.trim() === '' : true;

          if (isEmpty) {
            onDelete();
          } else {
            Vocabulary.add(tag);
            element.remove();
            that.fireEvent('submit', tag);
            return false;
          }
        },

        onDelete = function() {
          element.remove();
          that.fireEvent('delete');
          return false;
        },

        onCancel = function() {
          element.remove();
          that.fireEvent('cancel');
          return false;
        },

        onKeydown = function(e) {
          if (e.which === 13) // Enter = Submit
            onSubmit();
          else if (e.which === 27 && opt_tag) // Escape on existing tag = Cancel
            onCancel();
          else if (e.which == 27) // Escape on new tag = Delete
            onDelete();
        };

    init();

    HasEvents.apply(this);
  };
  TagPopup.prototype = Object.create(HasEvents.prototype);

  return TagPopup;

});
