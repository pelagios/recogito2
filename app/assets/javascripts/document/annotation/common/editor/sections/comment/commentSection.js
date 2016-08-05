define([
  'document/annotation/common/editor/sections/section',
  'common/ui/formatting',
  'common/config'], function(Section, Formatting, Config) {

  var CommentSection = function(parent, commentBody, zIndex) {
    var self = this,

        /** TODO we may want to allow HTML later - but then need to sanitize **/
        escapeHtml = function(text) {
          return jQuery('<div/>').text(text).html();
        },

        // TODO replace hard-wired z-index with number derived from sibling count
        element = jQuery(
          '<div class="section comment">' + // style="z-index:' + zIndex + '">' +
            '<div class="comment-body">' + escapeHtml(commentBody.value) + '</div>' +
            '<div class="icon edit">&#xf142;</div>' +
            '<div class="last-modified">' +
              '<a class="by" href="/' + commentBody.last_modified_by + '">' +
                commentBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(commentBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>'),

          commentDiv = element.find('.comment-body'),

          created = element.find('.created'),

          btnOpenDropdown = element.find('.edit'),

          dropdownMenu = jQuery(
            '<ul class="edit-dropdown">' +
              '<li data-fn="edit">Edit</li>' +
              '<li data-fn="delete">Delete</li>' +
            '</ul>'),

          toggleEditDropdown = function(e) {
            if (e.target === btnOpenDropdown[0]) {
              // Click on this button - toggle
              if (btnOpenDropdown.hasClass('focus')) {
                btnOpenDropdown.removeClass('focus');
                dropdownMenu.hide();
              } else {
                btnOpenDropdown.addClass('focus');
                dropdownMenu.show();
              }
            } else {
              // Click anywhere else - close
              btnOpenDropdown.removeClass('focus');
              dropdownMenu.hide();
            }
          },

          enableEditDropdown = function() {
            // Append the menu element
            dropdownMenu.hide();
            element.append(dropdownMenu);

            // Need to handle this as global event, so we can:
            // - close when user clicks outside
            // - have radio-button like behaviour for multiple comments
            jQuery(document).on('click', toggleEditDropdown);

            // Make menu items clickable
            dropdownMenu.on('click', 'li', function(e) {
              var fn = jQuery(e.target).data('fn');
              if (fn === 'delete')
                self.fireEvent('delete');
              else
                makeEditable();
            });
          },

          // Cf. http://stackoverflow.com/questions/13513329/set-cursor-to-the-end-of-contenteditable-div
          placeCaretAtEnd = function(element) {
            var range = document.createRange(),
                selection = window.getSelection();

            range.setStart(element[0], 1);
            range.collapse(true);
            selection.removeAllRanges();
            selection.addRange(range);
            element.focus();
          },

          makeEditable = function() {
            element.addClass('editable');
            commentDiv.prop('contenteditable', true);
            placeCaretAtEnd(commentDiv);
            created.remove();

            commentDiv.keyup(function(e) {
              if (e.ctrlKey && e.keyCode == 13)
                self.fireEvent('submit');
            });
          },

          hasChanged = function() {
            var initialContent = commentBody.value,
                currentContent = commentDiv.text().trim();

            return initialContent !== currentContent;
          },

          commit = function() {
            if (hasChanged()) {
              delete commentBody.last_modified_at;
              commentBody.last_modified_by = Config.me;
              commentBody.value = commentDiv.text().trim();
            }
          },

          destroy = function() {
            jQuery(document).off('click', toggleEditDropdown);
            element.remove();
          };

    if (Config.writeAccess && commentBody.last_modified_by === Config.me)
      enableEditDropdown();
    else
      element.find('.edit').hide();

    parent.append(element);

    this.body = commentBody;
    this.hasChanged = hasChanged;
    this.commit = commit;
    this.destroy = destroy;

    Section.apply(this);
  };
  CommentSection.prototype = Object.create(Section.prototype);

  return CommentSection;

});
