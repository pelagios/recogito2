define([
  'common/utils/annotationUtils'
], function(AnnotationUtils) {

  var ReapplyPrompt = function(phraseAnnotator, annotations) {

    var element = jQuery(
          '<div class="clicktrap">' +
            '<div class="alert info reapply">' +
              '<h1>Re-Apply</h1>' +
              '<p class="message"></p>' +
              '<p class="buttons"></p>' +
            '</div>' +
          '</div>').hide().appendTo(document.body),

        getMessage = function(unannotated, annotated, quote) {
          var message;

          if (unannotated + annotated != 0) {
            if (annotated == 0) {
              if (unannotated == 1)
                message = 'There is 1 more un-annotated occurrence of <em>';
              else
                message = 'There are ' + unannotated + ' more un-annotated occurrences of <em>';
            } else if (unannotated == 0) {
              if (unannotated == 1)
                message = 'There is 1 other annotated occurrence of <em>';
              else
                message = 'There are ' + annotated + ' other annotated occurrences of <em>';
            } else {
              message = 'There are ' + unannotated + ' more un-annotated and ' + annotated + ' annotated occurrences of <em>';
            }

            message += quote + '</em> in the text. Do you want to re-apply this annotation?';
          }

          return message;
        },

        getButtons = function(annotated) {
          if (annotated > 0)
            return '<div class="stacked">' +
              '<button class="btn ok">YES &mdash; replace existing annotations</button>' +
              '<button class="btn ok">YES &mdash; merge with existing annotations</button>' +
              '<button class="btn outline cancel">NO &mdash; don\'t re-apply</button>' +
            '</div>';
          else
            return '<div>' +
              '<button class="btn ok">YES</button>' +
              '<button class="btn outline cancel">NO</button>' +
            '</div>';
        },

        open = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),

              unannotatedCount = phraseAnnotator.countOccurrences(quote),
              annotated = annotations.filterByQuote(quote),

              message = getMessage(unannotatedCount, annotated.length, quote),
              buttons = getButtons(annotated.length);

          element.find('p.message').html(message);
          element.find('p.buttons').html(buttons);
          element.show();
        },

        reapply = function(annotation) {
          var selections = phraseAnnotator.createSelections(annotation);
          self.onCreateAnnotationBatch(selections);
        };

    this.open = open;
    this.reapply = reapply;
  };

  return ReapplyPrompt;

});
