define(['marked'], function(marked) {

  var Attribution = function(el) {
    var popup = jQuery(
          '<div class="attribution clicktrap">' +
            '<div class="modal-wrapper">' +
              '<div class="attribution-modal">' +
                '<button class="nostyle outline-icon hide">&#xe897;</button>' +
                  '<span>' + marked(el.find('.text').html()) + '</span>' +
                '</div>' +
              '</div>' +
            '</div>').appendTo(document.body).hide(),

        btnShow = el.find('.show'),
        btnHide = popup.find('.hide'),

        modal = popup.find('.attribution-modal'),

        init = function() {
          // Remove the original text span
          el.find('.text').remove();
          btnShow.click(show);
          btnHide.click(hide);

          // Click on clicktrap should close the popup, but click on modal
          // should do nothing except open links in new window
          modal.click(function(e) {
            var target = jQuery(e.target).closest('a');
            if (target.length > 0)
              window.open(target.attr('href'));
            return false;
          });
          popup.click(hide);
        },

        show = function() {
          popup.show();
          return false;
        },

        hide = function() {
          popup.hide();
          return false;
        };

    init();
  };

  return Attribution;

});
