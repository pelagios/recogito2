define([
  'document/annotation/common/editor/sections/editableTextSection',
  'common/ui/formatting',
  'common/config'], function(EditableTextSection, Formatting, Config) {

  var CommentSection = function(parent, commentBody) {
    var self = this,

        // TODO replace hard-wired z-index with number derived from sibling count
        element = jQuery(
          '<div class="section editable-text comment">' +
            '<div class="text"></div>' +
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

          lastModified = element.find('.last-modified'),

          btnOpenDropdown = element.find('.edit'),

          dropdownMenu = jQuery(
            '<ul class="edit-dropdown">' +
              '<li data-fn="edit">Edit</li>' +
              '<li data-fn="delete">Delete</li>' +
            '</ul>'),

          /**
           * To place the drop-down menu on top of the comment fields, we need to apply
           * a z-index in the inverse order: the first comment gets z-index 9999, second
           * 9998 a.s.o.
           */
          setZIndex = function() {
            element.css('z-index', 10000 - element.index());
          },

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
                self.makeEditable();
            });
          };

    if (Config.writeAccess && commentBody.last_modified_by === Config.me)
      enableEditDropdown();
    else
      element.find('.edit').hide();

    parent.append(element);
    setZIndex();

    EditableTextSection.apply(this, [ element, commentBody ]);

    this.toggleEditDropdown = toggleEditDropdown;
  };
  CommentSection.prototype = Object.create(EditableTextSection.prototype);

  /** Extends the base destroy method **/
  CommentSection.prototype.destroy = function() {
    jQuery(document).off('click', this.toggleEditDropdown);
    EditableTextSection.prototype.destroy.call(this);
  };

  return CommentSection;

});
