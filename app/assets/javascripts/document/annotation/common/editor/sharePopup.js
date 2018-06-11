define(['common/config'], function(Config) {

  var l = window.location,

      BASE_URL = l.protocol + '//' +
        l.host + '/document/' +
        Config.documentId + '/part/' +
        Config.partSequenceNo + '/edit#';

  var SharePopup = function(parent) {
    var element = jQuery(
          '<div class="share-popup">' +
            '<div class="arrow"></div>' +
            '<span class="label">Copy URL to share</span>' +
            '<input type="text" class="annotation-link" />' +
          '</div>').hide().appendTo(parent),

        inputEl = element.find('input'),

        close = function() {
          element.hide();
        },

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else
            element.show();
        },

        setAnnotation = function(annotation) {
          inputEl.val(BASE_URL + annotation.annotation_id);
        };

    this.close = close;
    this.setAnnotation = setAnnotation;
    this.toggle = toggle;
  };

  return SharePopup;

});
