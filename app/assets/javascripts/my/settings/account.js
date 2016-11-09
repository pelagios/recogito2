require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/alert'], function(Alert) {

  jQuery(document).ready(function() {
    var btnDelete = jQuery('.delete-account .btn'),

        deleteAccount = function() {
          var alert = new Alert(
                Alert.WARNING,
                '<span class="icon">&#xf071;</span> Delete Account',
                '<strong>Are you absolutely sure you want to do this?</strong>'
              ),

              executeDelete = function() {
                jsRoutes.controllers.my.settings.AccountSettingsController.deleteAccount().ajax()
                  .done(function(response) {

                    // TODO say goodbye

                    btnDelete.removeClass('disabled');
                  });
              };

          btnDelete.addClass('disabled');
          alert.on('ok', executeDelete);
        };

    btnDelete.click(deleteAccount);
  });

});
