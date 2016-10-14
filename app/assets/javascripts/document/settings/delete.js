require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/alert',
  'common/config'
], function(Alert, Config) {

  jQuery(document).ready(function() {

    var btnDeleteAnnotations = jQuery('.delete-annotations .btn'),

        deleteAnnotationsEnabled = function() {
          return !btnDeleteAnnotations.hasClass('disabled');
        },

        deleteAnnotations = function() {
          if (deleteAnnotationsEnabled()) {
            var warningTitle = '<span class="icon">&#xf071;</span> Delete All Annotations',
                warningMsg = '<strong>Are you absolutely sure you want to do this?</strong>',

                executeDelete = function() {
                  jsRoutes.controllers.document.settings.SettingsController.deleteAnnotations(Config.documentId).ajax()
                    .done(function(response) {
                      btnDeleteAnnotations.removeClass('disabled');
                    });
                },

                resetButton = function() {
                  btnDeleteAnnotations.removeClass('disabled');
                },

                alert = new Alert(Alert.WARNING, warningTitle, warningMsg);

            btnDeleteAnnotations.addClass('disabled');

            alert.on('ok', executeDelete);
            alert.on('cancel', resetButton);
          }
        };

    btnDeleteAnnotations.click(deleteAnnotations);

  });

});
