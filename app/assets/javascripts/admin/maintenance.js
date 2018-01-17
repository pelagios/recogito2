require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function(Formatting) {

  jQuery(document).ready(function() {

    var deleteOne = function(e) {
          alert('Not implemented yet.');
          return false;
        },

        deleteAll = function() {
          var reload = function() {
                location.reload(true);
              };

          jsRoutes.controllers.admin.maintenance.MaintenanceController.deleteAllPending()
            .ajax().done(reload);

          return false;
        };

    jQuery(document.body).on('click', '.delete-one', deleteOne);
    jQuery(document.body).on('click', '.delete-all', deleteAll);
  });

});
