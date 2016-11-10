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
                var redirectToSplashpage = function() {
                      window.location.replace('/');
                    };

                jsRoutes.controllers.my.settings.AccountSettingsController.deleteAccount().ajax()
                  .done(function(response) {
                    var alert = new Alert(
                      Alert.INFO,
                      'Good Bye',
                      'Good Bye &amp; thanks for checking out Recogito. We appreciate your feedback ' +
                      'via the <a href="http://commons.pelagios.org/groups/recogito-users/">Recogito Users group</a>.'
                    );

                    alert.on('ok', redirectToSplashpage);
                  })
                  .fail(function(error) {
                    new Alert(
                      Alert.ERROR,
                      'Error',
                      'An error occured while deleting your account.'
                    );
                  });
              };

          btnDelete.addClass('disabled');
          alert.on('ok', executeDelete);
        };

    btnDelete.click(deleteAccount);
  });

});
