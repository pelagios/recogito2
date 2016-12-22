define([], function() {

  var BaseBulkEditor = function(title, body) {
    var element = jQuery(
          '<div class="bulk-annotation-editor clicktrap">' +
            '<div class="modal-wrapper">' +
              '<div class="modal">' +
                '<div class="modal-header">' +
                  '<h2>' + title + '</h2>' +
                  '<button class="nostyle outline-icon cancel">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body"></div>' +
              '</div>' +
            '</div>' +
          '</div>'),

        btnCancel = element.find('.cancel'),

        init = function() {
          // 'X' icon click handler
          btnCancel.click(destroy);

          // Attach element to DOM and make draggable
          jQuery(document.body).append(element);
          element.find('.modal-body').append(body);
          element.find('.modal-wrapper').draggable({ handle: '.modal-header' });
        },

        destroy = function() {
          element.remove();
        };

    init();
  };

  return BaseBulkEditor;

});
