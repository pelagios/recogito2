require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/config'], function(Config) {

  var PartMetadataEditor = function() {
    var element = jQuery(
          '<div class="part-metadata-editor clicktrap">' +
            '<div class="modal-wrapper">' +
              '<div class="modal">' +

                '<div class="modal-header">' +
                  '<h2>Part Metadata</h2>' +
                  '<button class="nostyle outline-icon cancel">&#xe897;</button>' +
                '</div>' +

                '<div class="modal-body">' +
                '</div>' +

              '</div>' +
            '</div>' +
          '</div>'),

        btnCancel = element.find('.cancel'),

        destroy = function() {
          element.remove();
        };

    btnCancel.click(destroy);

    jQuery(document.body).append(element);
  };


  jQuery(document).ready(function() {

    var partList = jQuery('.part-metadata ul'),

        parts = jQuery('li.filepart'),

        flashMessage = jQuery('.part-metadata .flash-message'),

        clearFlashMessage = function() {
          flashMessage.hide();
        },

        setFlashMessage = function(cssClass, html) {
          flashMessage.removeClass();
          flashMessage.addClass(cssClass + ' flash-message');
          flashMessage.html(html);
          flashMessage.show();
        },

        onOrderChanged = function() {
          var sortOrder = jQuery.map(parts, function(li) {
                var part = jQuery(li);
                return { id: part.data('id'), sequence_no: part.index() + 1 };
              });

          jsRoutes.controllers.document.settings.SettingsController.setSortOrder(Config.documentId).ajax({
            data: JSON.stringify(sortOrder),
            contentType: 'application/json'
          }).success(function(error) {
            setFlashMessage('success', '<span class="icon">&#xf00c;</span> Your settings have been saved.');
          }).fail(function(error) {
            setFlashMessage('error', '<span class="icon">&#xf00d;</span> ' + error);
          });
        },

        onOpenPartEditor = function(e) {
          console.log(e);
          var editor = new PartMetadataEditor();
        };

    // Make filepart elements sortable (and disable selection)
    partList.disableSelection();
    partList.sortable({
      start: clearFlashMessage,
      stop: onOrderChanged
    });

    // 'Edit part metadata' button handler
    partList.on('click', 'button', onOpenPartEditor);
  });

});
