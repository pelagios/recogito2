define([], function() {

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
      };

  return {

    // TODO escape angle brackets
    format : function(contribution) {
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
            .resolveAnnotationView(contribution.affects_item.document_id, contribution.affects_item.filepart_id, contribution.affects_item.annotation_id).url;
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
    }

  };

});
