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
  var original = jQuery.fn.val,

      escapeHtml = function(text) {
        return jQuery('<div/>').text(text).html();
      };

  jQuery.fn.val = function() {
    if ($(this).is('*[contenteditable=true]'))
      return jQuery.fn.text.apply(this, arguments);
    return original.apply(this, arguments);
  };

  var TagPopup = function(containerEl) { //, position, opt_tag) {

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

        connection,

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

          inputEl.keydown(onKeydown);
          inputEl.typeahead({ hint:false },{ source: matcher });

          btnDelete.click(onDelete);
          btnOk.click(onSubmit);
        },

        open = function(position, conn) {
          connection = conn;

          element.css({ top: position[1] - 15, left: position[0] });
          element.show();

          inputEl.empty();

          if (conn.isStored()) {
            inputEl.html(escapeHtml(conn.getLabel()));
            inputEl.focus();
          } else {
            setTimeout(function() { inputEl.focus(); }, 1);
          }
        },

        onSubmit = function() {
          var tag = inputEl.val().trim(),
              isEmpty = (tag) ? tag === '' : true;

          if (isEmpty) {
            onDelete();
          } else {
            connection.setLabel(tag);
            Vocabulary.add(tag);
            element.hide();
            that.fireEvent('submit', connection);
            return false;
          }
        },

        onDelete = function() {
          element.hide();

          if (connection.isStored())
            that.fireEvent('delete', connection);
          else // Delete on a new connection = cancel
            that.fireEvent('cancel', connection);

          connection.destroy();

          return false;
        },

        onCancel = function() {
          element.hide();
          that.fireEvent('cancel', connection);
          return false;
        },

        onKeydown = function(e) {
          if (e.which === 13) // Enter = Submit
            onSubmit();
          else if (e.which === 27 && connection.isStored()) // Escape on stored connection = Cancel
            onCancel();
          else if (e.which === 27) // Escape on new connection = Delete
            onDelete();
        },

        close = function() {
          element.hide();
        };

    init();

    this.open = open;
    this.close = close;

    HasEvents.apply(this);
  };
  TagPopup.prototype = Object.create(HasEvents.prototype);

  return TagPopup;

});
