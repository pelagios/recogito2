require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/ui/formatting', 'common/config'], function(Formatting, Config) {
  var formatAction = function(contribution) {
        var action = contribution.action,
            itemType = contribution.affects_item.item_type,
            valBefore = contribution.affects_item.value_before,
            valAfter = contribution.affects_item.value_after;

        if (action === 'CREATE_BODY' && itemType === 'QUOTE_BODY')
          return 'Created annotation ' + valAfter;
        else if (action === 'CREATE_BODY' && itemType === 'COMMENT_BODY')
          return 'Created new comment';
        else if (action === 'CREATE_BODY' && itemType === 'PLACE_BODY')
          return 'Tagged as place';
        else if (action === 'CONFIRM_BODY')
          return 'Confirmed';
        else if (action === 'FLAG_BODY')
          return 'Flagged';
        else if (action === 'EDIT_BODY')
          return 'Changed something';
      },

      /** Checks if two dates are on the same UTC day **/
      isSameDayUTC = function(a, b) {
        var dateA = new Date(a),
            dateB = new Date(b),

            yearA  = dateA.getFullYear(),
            yearB  = dateB.getFullYear(),

            monthA = dateA.getMonth(),
            monthB = dateB.getMonth(),

            dayA   = dateA.getDate(),
            dayB   = dateB.getDate();

        return (yearA === yearB && monthA === monthB && dayA === dayB);
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

      history.items.reduce(function(previous, contribution) {
        var row =
              '<div class="contribution">' +
                '<p>' +
                  '<span class="action">' + formatAction(contribution) + '</span> ' +
                '</p>' +
              '</div>',
              ul, li;

        if (previous && previous.contribution.made_at === contribution.made_at) {
          // Contribution is part of the same edit - add to the previous <li>
          ul = previous.ul;
          li = previous.li;
          li.append(row);
        } else if (previous && isSameDayUTC(previous.contribution.made_at, contribution.made_at)) {
          // Different edit, but on the same day - render in new <li>
          ul = previous.ul;
          li = jQuery('<li></li>');
          li.append(row);
          ul.append(li);
        } else {
          // New edit on a new day - render in new <h3> and <ul>
          contributions.append('<h3>Edits on ' + Formatting.formatDay(new Date(contribution.made_at)) + '</h3>');
          ul = jQuery('<ul></ul>');
          li = jQuery('<li></li>');
          li.append(row);
          ul.append(li);
          contributions.append(ul);
        }

        return { contribution: contribution, ul: ul, li: li };
      }, false);
    });
  });

});
