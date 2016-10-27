require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/formatting'], function(Formatting) {

  jQuery(document).ready(function() {

    var totalEdits = jQuery('.total-edits .number'),
        totalAnnotations = jQuery('.total-annotations .number'),
        registeredUsers = jQuery('.registered-users .number'),

        refreshContributionStats = function() {
          var fillNumber = function(field, num) {
                field.html(Formatting.formatNumber(num));
              };

          jsRoutes.controllers.api.StatsAPIController.getDashboardStats().ajax().done(function(stats) {

            console.log(stats);

            fillNumber(totalEdits, stats.contributions.total_contributions);
            fillNumber(totalAnnotations, stats.total_annotations);
            fillNumber(registeredUsers, stats.total_users);
          });
        },

        refresh = function() {
          refreshContributionStats();
        };

    refresh();
  });

});
