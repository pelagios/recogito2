define(['common/config'], function(Config) {

  var l = window.location, // Shorthand

      BASE_URL = l.protocol + '//' +
        l.host + '/document/' +
        Config.documentId + '/part/' +
        Config.partSequenceNo + '/edit#';

  var SharePopup = function(parent) {
    var element = jQuery(
          '<div class="share-popup">' +
            '<div class="arrow"></div>' +
            '<span class="label"><span class="icon">&#xf0c1;</span> Link to share ' +
              '<span class="notifier">Copied to Clipboard</span>' +
            '</span>' +
            '<input type="text" class="annotation-link" />' +
          '</div>').hide().appendTo(parent),

        notifierEl = element.find('.notifier').hide(),

        inputEl = element.find('input'),

        close = function() {
          notifierEl.hide();
          element.hide();
        },

        isOpen = function() {
          return element.is(':visible');
        },

        toggle = function() {
          if (element.is(':visible'))
            close();
          else
            element.show();
        },

        setAnnotation = function(annotation) {
          inputEl.val(BASE_URL + annotation.annotation_id);
        },

        onFocus = function() {
          jQuery(this).select();
          document.execCommand('copy');
          notifierEl.fadeIn(100);
          setTimeout(function() { notifierEl.fadeOut(300); }, 2000);
        };

    inputEl.focus(onFocus);

    this.close = close;
    this.isOpen = isOpen;
    this.setAnnotation = setAnnotation;
    this.toggle = toggle;
  };

  return SharePopup;

});
