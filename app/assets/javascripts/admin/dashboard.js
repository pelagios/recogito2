require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/formatting'], function(Formatting) {

  jQuery(document).ready(function() {

    var totalEdits = jQuery('.total-edits .number'),
        totalAnnotations = jQuery('.total-annotations .number'),
        registeredUsers = jQuery('.registered-users .number'),

        topUsers = jQuery('.top-users table'),

        refreshHighscores = function(scores) {

          var maxScore = Math.max.apply(null, jQuery.map(scores, function(score) {
                return score.value;
              })),

              toPercent = function(score) {
                return score / maxScore * 100;
              },

              createRow = function(username, count) {
                return jQuery(
                  '<tr>' +
                    '<td><a href=/admin/users/' + username + '">' + username + '</a></td>' +
                    '<td>' +
                      '<div class="meter">' +
                        '<div class="bar" style="width:' + toPercent(count) + '%"></div>' +
                      '</div>' +
                    '</td>' +
                    '<td>' + Formatting.formatNumber(count) + ' Edits</td>' +
                  '</tr>');
              };

          jQuery.each(scores, function(idx, score) {
            topUsers.append(createRow(score.username, score.value));
          });
        },

        refreshContributionStats = function() {
          var fillNumber = function(field, num) {
                field.html(Formatting.formatNumber(num));
              };

          jsRoutes.controllers.api.StatsAPIController.getDashboardStats().ajax().done(function(stats) {
            fillNumber(totalEdits, stats.contributions.total_contributions);
            fillNumber(totalAnnotations, stats.total_annotations);
            fillNumber(registeredUsers, stats.total_users);

            refreshHighscores(stats.contributions.by_user);
          });
        },


        refresh = function() {
          refreshContributionStats();
        };

    refresh();
  });

});
