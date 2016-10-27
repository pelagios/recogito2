require([], function() {

  jQuery(document).ready(function() {
    var totalContributionsEl = (function() {
          var container = jQuery('#total-contributions'),
              template = jQuery(
                '<div>' +
                  '<div class="total-contributions-count"></div>' +
                  '<div class="total-contributions-caption">Total Contributions</div>' +
                '</div>');
          container.append(template);
          return container.find('.total-contributions-count');
        })(),

        updateTotalContributions = function(value) {
          totalContributionsEl.html(value);
        },

        updateByUser = function(buckets) {
          var x = jQuery.map(buckets, function(bucket) { return bucket.value; }),
              y = jQuery.map(buckets, function(bucket) { return bucket.username; }),
              data = [{ x: x, y: y, type: 'bar', orientation: 'h' }];
          Plotly.newPlot('by-user', data);
        },

        /** TODO dry! **/

        updateByAction = function(buckets) {
          var values = jQuery.map(buckets, function(bucket) { return bucket.value; }),
              labels = jQuery.map(buckets, function(bucket) { return bucket.action; }),
              data = [{ values: values, labels: labels, type: 'pie' }];
          Plotly.newPlot('by-action', data);
        },

        updateByItemType = function(buckets) {
          var values = jQuery.map(buckets, function(bucket) { return bucket.value; }),
              labels = jQuery.map(buckets, function(bucket) { return bucket.item_type; }),
              data = [{ values: values, labels: labels, type: 'pie' }];
          Plotly.newPlot('by-item-type', data);
        },

        updateContributionHistory = function(buckets) {
          var x = jQuery.map(buckets, function(bucket) { return new Date(bucket.date); }),
              y = jQuery.map(buckets, function(bucket) { return bucket.value; }),
              data = [{ x: x, y: y, type: 'scatter' }],
              layout = {
                xaxis: {
                  type: 'date',
                  dtick: 86400000.0,
                  tickformat: '%b %d'
                },
                margin: { l:50, r:50, b: 40, t: 30 }
              },
              config = {
                displayModeBar: false
              };
          Plotly.newPlot('contribution-history', data, layout, config);
        },

        updateAll = function() {
          jsRoutes.controllers.api.StatsAPIController.getDashboardStats().ajax().done(function(stats) {
            updateTotalContributions(stats.contributions.total_contributions);
            updateByUser(stats.contributions.by_user);
            updateByAction(stats.contributions.by_action);
            updateByItemType(stats.contributions.by_item_type);
            updateContributionHistory(stats.contributions.contribution_history);
          });
        };

    updateAll();
  });

});
