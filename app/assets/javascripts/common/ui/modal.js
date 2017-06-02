define(['common/hasEvents'], function(HasEvents) {

  var Modal = function(title, body, opt_cssClass) {

    var element = jQuery(
          '<div class="clicktrap">' +
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
          element.find('.modal-body').append(body);
          btnCancel.click(destroy);

          if (opt_cssClass)
            element.find('.modal').addClass(opt_cssClass);
        },

        destroy = function() {
          element.remove();
        };

    init();

    this.element = element;
    this.destroy = destroy;

    HasEvents.apply(this);
  };
  Modal.prototype = Object.create(HasEvents.prototype);

  Modal.prototype.open = function() {
    if (!jQuery.contains(document, this.element[0])) {
      jQuery(document.body).append(this.element);
      this.element.find('.modal-wrapper').draggable({ handle: '.modal-header' });
    }
  };

  return Modal;

});
