define(['common/config'], function(Config) {

  var l = window.location, // Shorthand

      ANNOTATION_BASE = l.protocol + '//' +
        l.host + '/annotation/',

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
              '<div class="url">' +
                '<input type="text" id="share-annotation" class="share-link annotation-url" />' +
                '<button class="to-clipboard" data-for="share-annotation">&#xf0ea;</button>' +
              '</div>' +

              '<span class="label"><span class="icon">&#xf03e;</span> Link to share image snippet' +
                '<span class="notifier" data-for="share-image">Copied to Clipboard</span>' +
              '</span>' +
              '<div class="url">' +
                '<input type="text" id="share-image" class="share-link image-url" />' +
                '<button class="to-clipboard" data-for="share-image">&#xf0ea;</button>' +
              '</div>' +

              SHARE_WARNING +

            '</div>').hide().appendTo(parent) :

          jQuery(
            '<div class="share-popup">' +
              '<div class="arrow"></div>' +

              '<span class="label"><span class="icon">&#xf0c1;</span> Link to share ' +
                '<span class="notifier" data-for="share-annotation">Copied to Clipboard</span>' +
              '</span>' +
              '<div class="url">' +
                '<input type="text" id="share-annotation" class="share-link annotation-url" />' +
                '<button class="to-clipboard" data-for="share-annotation">&#xf0ea;</button>' +
              '</div>' +

              SHARE_WARNING +

            '</div>').hide().appendTo(parent),

        annotationInput = element.find('input.annotation-url'),
        imageInput = element.find('input.image-url'),

        onClick = function(e) {
          var destination = jQuery(e.target).closest('.share-popup');
          if (destination.length === 0) close(); // Click outside
        },

        close = function() {
          parent.off('click', onClick);
          element.hide();
        },

        open = function() {
          jQuery('.notifier').hide();
          element.show();
          setTimeout(function() {
            // We need to delay, otherwise the opening click is already registered as an
            // outside click, instantly closing the popup again
            parent.on('click', onClick);
          }, 1);
        },

        isOpen = function() {
          return element.is(':visible');
        },

        toggle = function() {
          if (element.is(':visible')) close();
          else open();
        },

        setAnnotation = function(annotation) {
          var id = annotation.annotation_id;
          annotationInput.val(ANNOTATION_BASE + id);
          imageInput.val(IMAGE_BASE + id + '.jpg');
        },

        onCopy = function() {
          var button = jQuery(this),
              inputId = button.data('for'),
              inputEl = jQuery('#' + inputId),
              notifier = jQuery('.notifier[data-for="' + inputId + '"]');

          inputEl.select();
          document.execCommand('copy');
          inputEl.blur();

          notifier.fadeIn(100);
          setTimeout(function() { notifier.fadeOut(300); }, 2000);
        };

    element.find('button').click(onCopy);

    this.close = close;
    this.isOpen = isOpen;
    this.setAnnotation = setAnnotation;
    this.toggle = toggle;
  };

  return SharePopup;

});
