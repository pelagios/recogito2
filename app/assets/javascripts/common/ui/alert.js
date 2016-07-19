define(['common/hasEvents'], function(HasEvents) {

  var Alert = function(cssClass, title, message) {
    var self = this,

        element = jQuery(
          '<div class="clicktrap">' +
            '<div class="alert ' + cssClass + '">' +
              '<h1>' + title + '</h1>' +
              '<p>' + message + '</p>' +
              '<p class="buttons">' +
                '<button class="btn ok">OK</button>' +
                '<button class="btn outline cancel">Cancel</button>' +
              '</p>' +
            '</div>' +
          '</div>'),

        btnOK     = element.find('button.ok'),
        btnCancel = element.find('button.cancel'),

        onOK = function() {
          self.fireEvent('ok');
          element.remove();
        },

        onCancel = function() {
          self.fireEvent('cancel');
          element.remove();
        };

    btnOK.click(onOK);
    btnCancel.click(onCancel);

    jQuery(document.body).append(element);
    HasEvents.apply(this);
  };
  Alert.prototype = Object.create(HasEvents.prototype);

  return Alert;

});
