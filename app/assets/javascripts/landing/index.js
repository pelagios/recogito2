require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var widget = jQuery('.recogito-rightnow'),

        annotationsEl = widget.find('.annotations h3'),
        editsEl = widget.find('.edits h3'),
        usersEl = widget.find('.users h3'),

        format = function(num) {
          if (num > 10000)
            return Math.round(num / 1000) + 'K';
          else if (num > 1000)
            return Math.floor(num / 1000) + ',' + (num % 1000);
          else
            return num;
        },

        refreshStats = function() {
          jsRoutes.controllers.api.StatsAPIController.getRightNowStats().ajax()
            .done(function(response) {
              annotationsEl.html(format(response.annotations));
              editsEl.html(format(response.edits_today));
              usersEl.html(format(response.users));

              window.setTimeout(function() { refreshStats(); }, 5000);
            });
        };

    refreshStats();
  });

});
