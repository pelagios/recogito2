require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/config'], function(Formatting, PlaceUtils, Config) {

  var uriToLink = function(uri) {
        if (uri) {
          var parsed = PlaceUtils.parseURI(uri);
          if (parsed.shortcode)
            return '<a href="' + uri + '" target="_blank">' + parsed.shortcode + ':' + parsed.id + '</a>';
          else
            return '<a href="' + uri + '" target="_blank">' + uri + '</a>';
        } else {
          return '<em>[none]</em>';
        }
      },

      formatAction = function(contribution) {
        var action = contribution.action,
            itemType = contribution.affects_item.item_type,
            itemTypeLabel = (function() {
              if (itemType === 'PLACE_BODY')
                return 'place';
            })(),
            valBefore = contribution.affects_item.value_before,
            valBeforeShort = (valBefore && valBefore.length > 256) ? valBefore.substring(0, 256) + '...' : valBefore,
            valAfter = contribution.affects_item.value_after,
            valAfterShort = (valAfter && valAfter.length > 256) ? valAfter.substring(0, 256) + '...' : valAfter,
            context = contribution.context;

        if (action === 'CREATE_BODY' && itemType === 'QUOTE_BODY') {
          return 'Highlighted section <em>&raquo;' + valAfterShort + '&laquo;</em>';
        } else if (action === 'CREATE_BODY' && itemType === 'COMMENT_BODY') {
          return 'New comment <em>&raquo;' + valAfterShort + '&laquo;</em>';
        } else if (action === 'CREATE_BODY') {
          return 'Tagged <em>&raquo;' + context + '&laquo;</em> as ' + itemTypeLabel;
        } else if (action === 'CONFIRM_BODY') {
          return 'Confirmed <em>&raquo;' + context + '&laquo;</em> as ' + itemTypeLabel + ' ' + uriToLink(valAfter);
        } else if (action === 'FLAG_BODY') {
          return 'Flagged ' + itemTypeLabel + '<em>&raquo;' + context + '&laquo;</em>';
        } else if (action === 'EDIT_BODY' && itemType === 'QUOTE_BODY') {
          return 'Changed selection from <em>&raquo;' + valBeforeShort + '&laquo;</em> to <em>&raquo;' + valAfterShort + '&laquo;</em>';
        } else if (action === 'EDIT_BODY' && itemType === 'PLACE_BODY') {
          return 'Changed ' + itemTypeLabel + ' from ' + uriToLink(valBefore) + ' to ' + uriToLink(valAfter);
        } else if (action === 'DELETE_ANNOTATION') {
          return 'Deleted annotation <em>&raquo;' + context + '&laquo;</em>';
        } else {
          return 'An unknown change happend';
        }
      },

      /** Checks if two dates are on the same UTC day **/
      isSameDay = function(a, b) {
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
          window.setTimeout(function() { location.reload(); }, 1000);
        });
      };

  jQuery(document).ready(function() {

    jQuery('.edit-history').on('click', '.rollback', function(e) {
      var el = jQuery(e.target).closest('li'),
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
            annotationId = contribution.affects_item.annotation_id,
            versionId = contribution.affects_item.annotation_version_id,
            rollbackButton = '<button class="rollback icon" title="Revert document to this state">&#xf0e2;</button>',
            ul, li;

        if (previous && previous.contribution.made_at === contribution.made_at) {
          // Contribution is part of the same edit - add to the previous <li>
          ul = previous.ul;
          li = previous.li;
          li.append(row);
        } else if (previous && isSameDay(previous.contribution.made_at, contribution.made_at)) {
          // Different edit, but on the same day - render in new <li>
          ul = previous.ul;
          li = jQuery('<li data-annotation="' + annotationId + '" data-version="' + versionId + '">' + rollbackButton + '</li>');
          li.append(row);
          ul.append(li);
        } else {
          // New edit on a new day - render in new <h3> and <ul>
          contributions.append('<h3>Edits on ' + Formatting.formatDay(new Date(contribution.made_at)) + '</h3>');
          ul = jQuery('<ul></ul>');
          li = jQuery('<li data-annotation="' + annotationId + '" data-version="' + versionId + '">' + rollbackButton + '</li>');
          li.append(row);
          ul.append(li);
          contributions.append(ul);
        }

        return { contribution: contribution, ul: ul, li: li };
      }, false);
    });
  });

});
