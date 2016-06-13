require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/helpers/formatting', 'common/config'], function(Formatting, Config) {
  var formatAction = function(action) {
        if (action === 'CREATE_BODY')
          return '<span class="create">New</span>';
      },

      formatChange = function(item) {

      },

      formatItem = function(item) {
        if (item.item_type === 'QUOTE_BODY')
          return '<span class="annotation">annotation</span>';
        else if (item.item_type === 'COMMENT_BODY')
          return '<span class="comment">comment</span>';
        else if (item.item_type === 'PLACE_BODY')
          return '<span class="place">place</span>';
      };

  jQuery(document).ready(function() {
    jsRoutes.controllers.api.ContributionAPIController.getContributionHistory(Config.documentId).ajax().done(function(history) {
      var contributions = jQuery('.edit-history');

      jQuery.each(history.items, function(idx, contrib) {
        var li = jQuery(
          '<li class="contribution">'+
            '<p>' +
              formatAction(contrib.action) + ' ' +
              formatItem(contrib.affects_item) +
            '</p>' +
            '<p class="made">' +
              '<span class="by">' + contrib.made_by + '</span> ' +
              '<span class="at">' + Formatting.timeSince(contrib.made_at) + '</span>' +
            '</p>' +
          '</li>');

        console.log(contrib);
        contributions.append(li);
      });
    });
  });
});
