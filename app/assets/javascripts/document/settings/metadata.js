require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/config'], function(Config) {

  jQuery(document).ready(function() {

    var parts = jQuery('li.filepart'),

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
        };

    // Make filepart elements sortable (and disable selection)
    jQuery('.part-metadata ul').disableSelection();
    jQuery('.part-metadata ul').sortable({
      start: clearFlashMessage,
      stop: onOrderChanged
    });
  });

});
