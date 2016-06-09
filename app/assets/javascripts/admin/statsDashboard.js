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
          var x = jQuery.map(buckets, function(bucket) { return bucket.date; }),
              y = jQuery.map(buckets, function(bucket) { return bucket.value; }),
              data = [{ x: x, y: y, type: 'scatter' }];
          Plotly.newPlot('contribution-history', data);
        },

        updateAll = function() {
          jsRoutes.controllers.api.StatsAPIController.getGlobalStats().ajax().done(function(stats) {
            console.log(stats);
            updateTotalContributions(stats.total_contributions);
            updateByUser(stats.by_user);
            updateByAction(stats.by_action);
            updateByItemType(stats.by_item_type);
            updateContributionHistory(stats.contribution_history);
          });
        };

    updateAll();
  });

});
