define(['common/config'], function(Config) {

  var l = window.location, // Shorthand

      ANNOTATION_BASE = l.protocol + '//' +
        l.host + '/document/' +
        Config.documentId + '/part/' +
        Config.partSequenceNo + '/edit#',

      IMAGE_BASE = l.protocol + '//' +
        l.host + '/api/annotation/',

      SHARE_WARNING = (Config.isPublic) ? "" /* No warning */ :
        (Config.isAdmin) ?
          // Admin access -> link to sharing settings
          '<span class="private-document">' +
            '<span class="icon">&#xf071;</span> This document is not public ' +
            '(<a href="/document/' + Config.documentId + '/settings?tab=sharing" target="_blank">edit sharing settings</a>)' +
          '</span>' :

          // No admin? Link to help for more info
          '<span class="private-document">' +
            '<span class="icon">&#xf071;</span> This document is not public ' +
            '(<a href="/help/sharing-links" target="_blank">learn more</a>)' +
          '</span>';

  var SharePopup = function(parent) {

        // Design differs for text vs. image (images have a JPG share URL as well)
    var element = (Config.contentType.indexOf('IMAGE') === 0) ?
          jQuery(
            '<div class="share-popup">' +
              '<div class="arrow"></div>' +

              '<span class="label"><span class="icon">&#xf0c1;</span> Link to share annotation' +
                '<span class="notifier" data-for="share-annotation">Copied to Clipboard</span>' +
              '</span>' +
              '<input type="text" id="share-annotation" class="share-link annotation-url" />' +

              '<span class="label"><span class="icon">&#xf03e;</span> Link to share image snippet' +
                '<span class="notifier" data-for="share-image">Copied to Clipboard</span>' +
              '</span>' +
              '<input type="text" id="share-image" class="share-link image-url" />' +

              SHARE_WARNING +

            '</div>').hide().appendTo(parent) :

          jQuery(
            '<div class="share-popup">' +
              '<div class="arrow"></div>' +

              '<span class="label"><span class="icon">&#xf0c1;</span> Link to share ' +
                '<span class="notifier" data-for="share-annotation">Copied to Clipboard</span>' +
              '</span>' +
              '<input type="text" id="share-annotation" class="share-link annotation-url" />' +

              SHARE_WARNING +

            '</div>').hide().appendTo(parent),

        annotationInput = element.find('input.annotation-url'),
        imageInput = element.find('input.image-url'),

        close = function() {
          jQuery('.notifier').hide();
          element.hide();
        },

        isOpen = function() {
          return element.is(':visible');
        },

        toggle = function() {
          if (element.is(':visible')) close();
          else element.show();
        },

        setAnnotation = function(annotation) {
          var id = annotation.annotation_id;
          annotationInput.val(ANNOTATION_BASE + id);
          imageInput.val(IMAGE_BASE + id + '.jpg');
        },

        onFocus = function() {
          var el = jQuery(this),
              id = el.attr('id'),
              notifier = jQuery('.notifier[data-for="' + id + '"]');

          el.select();
          document.execCommand('copy');
          notifier.fadeIn(100);
          setTimeout(function() { notifier.fadeOut(300); }, 2000);
        };

    annotationInput.focus(onFocus);
    imageInput.focus(onFocus);

    this.close = close;
    this.isOpen = isOpen;
    this.setAnnotation = setAnnotation;
    this.toggle = toggle;
  };

  return SharePopup;

});
