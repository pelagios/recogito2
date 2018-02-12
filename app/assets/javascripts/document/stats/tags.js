require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/config',
  'common/ui/formatting'
], function(Config, Formatting) {

  jQuery(document).ready(function() {

    var loadTags = function() {
          return  jsRoutes.controllers.document.stats.StatsController.getTagsAsJSON(Config.documentId).ajax();
        },

        renderBars = function(listEl, totalEl) {

          return function(buckets) {

            var maxCount = Math.max.apply(null, buckets.map(function(b) {
                  return b.count;
                })),

                toPercent = function(count) {
                  return count / maxCount * 100;
                };

            totalEl.html('(' + buckets.length + ' distinct)');

            buckets.forEach(function(t) {
              listEl.append(
                '<tr>' +
                  '<td>' + t.value + '</td>' +
                  '<td>'  +
                    '<div class="meter">' +
                      '<div class="bar" style="width:' + toPercent(t.count) + '%"></div>' +
                    '</div>' +
                  '</td>' +
                  '<td>' + Formatting.formatNumber(t.count) + '</td>' +
                '</tr>');
            });
          };

      };

    loadTags().done(renderBars(
      jQuery('.tags-by-count'),
      jQuery('.tags-total')
    ));
  });

});
