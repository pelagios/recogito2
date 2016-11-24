require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/formatting',
  'common/utils/contributionUtils'
], function(Formatting, ContributionUtils) {

  var REFRESH_INTERVAL_MS = 1000;

  jQuery(document).ready(function() {

    var totalEdits = jQuery('.total-edits .number'),
        totalAnnotations = jQuery('.total-annotations .number'),
        totalVisits = jQuery('.total-visits .number'),
        registeredUsers = jQuery('.registered-users .number'),

        topContributors = jQuery('.top-contributors table'),

        rightNow = jQuery('.right-now table'),

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
                        '<div class="bar rounded" style="width:' + toPercent(count) + '%"></div>' +
                      '</div>' +
                    '</td>' +
                    '<td>' + Formatting.formatNumber(count) + ' Edits</td>' +
                  '</tr>');
              };

          topContributors.empty();
          jQuery.each(scores, function(idx, score) {
            topContributors.append(createRow(score.username, score.value));
          });
        },

        refreshContributionsRightNow = function(recentContributions) {

          console.log(recentContributions);
          
          // TODO optimize. We don't need to clear list every time
          rightNow.empty();
          jQuery.each(recentContributions, function(idx, contribution) {
            var odd = idx % 2,
                row = jQuery(
                  '<tr>' +
                    '<td>' + ContributionUtils.format(contribution) + '</td>' +
                    '<td></td>' + // TODO document
                    '<td class="made-at">' + Formatting.timeSince(contribution.made_at) + '</td>' +
                  '</tr>');

            if (!odd)
              row.addClass('even');

            rightNow.append(row);
          });
        },

        refreshContributionStats = function() {
          var fillNumber = function(field, num) {
                field.html(Formatting.formatNumber(num));
              };

          jsRoutes.controllers.api.StatsAPIController.getDashboardStats().ajax().done(function(stats) {
            fillNumber(totalEdits, stats.contribution_stats.total_contributions);
            fillNumber(totalAnnotations, stats.total_annotations);
            fillNumber(totalVisits, stats.total_visits);
            fillNumber(registeredUsers, stats.total_users);

            refreshContributionsRightNow(stats.recent_contributions);
            refreshHighscores(stats.contribution_stats.by_user);
          });
        },

        refresh = function() {
          refreshContributionStats();
          window.setTimeout(refresh, REFRESH_INTERVAL_MS);
        };

    refresh();
  });

});
