define(['../../../../common/formatting', '../../../../common/config',], function(Formatting, Config) {

  var CommentSection = function(parent, commentBody, zIndex) {
    var element = jQuery(
          '<div class="section comment" style="z-index:' + zIndex + '">' +
            '<div class="comment-body">' + commentBody.value + '</div>' +
            '<div class="icon edit">&#xf142;</div>' +
            '<div class="created">' +
              '<a class="by" href="/' + commentBody.last_modified_by + '">' +
                commentBody.last_modified_by +
              '</a>' +
              '<span class="at">' +
                Formatting.timeSince(commentBody.last_modified_at) +
              '</span>' +
            '</div>' +
          '</div>'),

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
              console.log(fn);
            });
          },

          destroy = function() {
            jQuery(document).off('click', toggleEditDropdown);
            element.remove();
          };

    if (commentBody.last_modified_by === Config.me)
      enableEditDropdown();
    else
      element.find('.edit').hide();

    parent.append(element);

    this.destroy = destroy;
  };

  return CommentSection;

});
