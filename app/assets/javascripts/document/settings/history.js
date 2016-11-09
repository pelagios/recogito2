require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/alert',
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/config'], function(Alert, Formatting, PlaceUtils, Config) {

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
            user = '<a href="/' + contribution.made_by + '">' + contribution.made_by + '</a> ',
            itemType = contribution.affects_item.item_type,
            itemTypeLabel = (function() {
              if (itemType === 'PLACE_BODY')
                return 'place';
            })(),
            valBefore = contribution.affects_item.value_before,
            valBeforeShort = (valBefore && valBefore.length > 256) ? valBefore.substring(0, 256) + '...' : valBefore,
            valAfter = contribution.affects_item.value_after,
            valAfterShort = (valAfter && valAfter.length > 256) ? valAfter.substring(0, 256) + '...' : valAfter,
            annotationUri = jsRoutes.controllers.document.annotation.AnnotationController
              .resolveAnnotationView(Config.documentId, contribution.affects_item.filepart_id, contribution.affects_item.annotation_id).url;
            context = (contribution.context) ? '<em>&raquo;<a href="' + annotationUri +'">' + contribution.context + '&laquo;</a></em>' : false;

        if (action === 'CREATE_BODY' && itemType === 'QUOTE_BODY') {
          return user + 'highlighted section ' + context;
        } else if (action === 'CREATE_BODY' && itemType === 'COMMENT_BODY') {
          return 'New comment by ' + user + ' <em>&raquo;' + valAfterShort + '&laquo;</em>';
        } else if (action === 'CREATE_BODY' && itemType === 'TRANSCRIPTION') {
          return user + 'added transcription <em>&raquo;' + valAfter + '&laquo;</em>';
        } else if (action === 'CREATE_BODY' && itemType === 'TAG_BODY') {
          return user + ' tagged ' + context + ' with <em>&raquo;' + valAfter + '&laquo;</em>';
        } else if (action === 'CREATE_BODY') {
          return user + 'tagged ' + context + ' as ' + itemTypeLabel;
        } else if (action === 'CONFIRM_BODY') {
          return user + 'confirmed ' + context + ' as ' + itemTypeLabel + ' ' + uriToLink(valAfter);
        } else if (action === 'FLAG_BODY') {
          return user + 'flagged ' + itemTypeLabel + ' ' + context;
        } else if (action === 'EDIT_BODY' && itemType === 'QUOTE_BODY') {
          return user + 'changed selection from <em>&raquo;' + valBeforeShort + '&laquo;</em> to <em>&raquo;' + valAfterShort + '&laquo;</em>';
        } else if (action === 'EDIT_BODY' && itemType === 'PLACE_BODY') {
          return user + 'changed ' + itemTypeLabel + ' from ' + uriToLink(valBefore) + ' to ' + uriToLink(valAfter);
        } else if (action === 'DELETE_ANNOTATION') {
          return user + 'deleted annotation <em>&raquo;' + contribution.context + '&laquo;</em>';
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

      rollback = function(contributionId) {
        jsRoutes.controllers.document.settings.SettingsController.rollbackByTime(Config.documentId, contributionId).ajax()
        .done(function(response) {
          window.setTimeout(function() { location.reload(); }, 1000);
        });
      };

  jQuery(document).ready(function() {

    jQuery('.edit-history').on('click', '.rollback', function(e) {
      var el = jQuery(e.target).closest('li'),
          contributionId = el.data('id'),
          warningTitle = '<span class="icon">&#xf071;</span> Revert Annotation History',
          warningMsg = 'You are about to revert the annotation history. ' +
            'This will permanently delete all edits that happened after the selected time. ' +
            'The operation is not reversible! <strong>Are you sure you want to do this?</strong>';

      new Alert(Alert.WARNING, warningTitle, warningMsg).on('ok', function() {
        rollback(contributionId);
      });
    });

    jsRoutes.controllers.api.StatsAPIController.getContributionHistory(Config.documentId).ajax().done(function(history) {
      var contributions = jQuery('.edit-history');

      history.items.reduce(function(previous, contribution) {
        var row =
              '<div class="contribution">' +
                '<p>' +
                  '<span class="action">' + formatAction(contribution) + '</span> ' +
                '</p>' +
              '</div>',
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
          li = jQuery('<li data-id="' + contribution.id + '">' + rollbackButton + '</li>');
          li.append(row);
          ul.append(li);
        } else {
          // New edit on a new day - render in new <h3> and <ul>
          contributions.append('<h3>Edits on ' + Formatting.formatDay(new Date(contribution.made_at)) + '</h3>');
          ul = jQuery('<ul></ul>');
          li = jQuery('<li data-id="' + contribution.id + '">' + rollbackButton + '</li>');
          li.append(row);
          ul.append(li);
          contributions.append(ul);
        }

        return { contribution: contribution, ul: ul, li: li };
      }, false);
    });
  });

});
