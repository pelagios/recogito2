require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/api',
  'common/config',
  'common/ui/formatting'
], function(API, Config, Formatting) {

  jQuery(document).ready(function() {

    var initPlugins = function() {
          Promise.all([
            API.listAnnotationsInDocument(Config.documentId),
            API.listPlacesInDocument(Config.documentId, 0, 10000)
          ]).then(function(values) {
            var args = {
              annotations: values[0],
              entities: values[1].items
            };

            for (var name in window.plugins) {
              if (window.plugins.hasOwnProperty(name))
                window.plugins[name](args);
            }
          });
        },

        loadTags = function() {
          return jsRoutes.controllers.document.stats.StatsController.getTagsAsJSON(Config.documentId).ajax();
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

    loadTags()
      .then(renderBars(
        jQuery('.tags-by-count'),
        jQuery('.tags-total')
      ))
      .then(initPlugins);
  });

});
