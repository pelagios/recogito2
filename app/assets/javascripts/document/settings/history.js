require(['../../common/ui/formatting', '../../common/config'], function(Formatting, Config) {
  var formatAction = function(action) {
        if (action === 'CREATE_BODY')
          return '<span class="create">New</span>';
      },

      formatChange = function(item) {
        var html = '<span class="change">';
        if (item.value_before && item.value_after)
          // TODO truncate changes (e.g. in case of long comments)
          html += 'from <em>&quot;' +  item.value_before + '&quot;</em> to ' +
            '<em>&quot;' + item.value_after + '&quot;</em>';
        else if (item.value_before)
          // Delete action
          html += '<em>&quot;' + item.value_before + '&quot;</em>';
        else if (item.value_after)
          html += '<em>&quot;' + item.value_after + '&quot;</em>';

        return html + '</span>';
      },

      formatItem = function(item) {
        formatChange(item);
        if (item.item_type === 'QUOTE_BODY')
          return '<span class="annotation">annotation ' + formatChange(item) + '</span>';
        else if (item.item_type === 'COMMENT_BODY')
          return '<span class="comment">comment ' + formatChange(item) + '</span>';
        else if (item.item_type === 'PLACE_BODY')
          return '<span class="place">place ' + formatChange(item) + '</span>';
      },

      rollback = function(annotationId, versionId) {
        jsRoutes.controllers.document.settings.SettingsController.rollbackByTime(Config.documentId).ajax({
          contentType: 'application/json',
          data: JSON.stringify({ annotation_id: annotationId, version_id: versionId })
        }).done(function(response) {
          window.setTimeout(function() { location.reload(); }, 500);
        });
      };

  jQuery(document).ready(function() {

    jQuery('.edit-history').on('click', '.rollback', function(e) {
      var el = jQuery(e.target),
          annotationId = el.data('annotation'),
          versionId = el.data('version');

      rollback(annotationId, versionId);
    });

    jsRoutes.controllers.api.ContributionAPIController.getContributionHistory(Config.documentId).ajax().done(function(history) {
      var contributions = jQuery('.edit-history');

      jQuery.each(history.items, function(idx, contrib) {
        var li = jQuery(
          '<tr>'+
            '<td class="contrib-info">' +
              '<p>' +
                formatAction(contrib.action) + ' ' +
                formatItem(contrib.affects_item) +
              '</p>' +
              '<p class="made">' +
                '<span class="by">' + contrib.made_by + '</span> ' +
                '<span class="at">' + Formatting.timeSince(contrib.made_at) + '</span>' +
              '</p>' +
            '</td>' +
            '<td class="action rollback icon" data-annotation="' +
              contrib.affects_item.annotation_id + '" data-version="' +
              contrib.affects_item.annotation_version_id + '">&#xf0e2;</td>' +
          '</tr>');

        contributions.append(li);
      });
    });
  });
});
