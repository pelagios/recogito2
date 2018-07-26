define([
  'common/utils/annotationUtils'
], function(AnnotationUtils) {

  var Reapply = function(phraseAnnotator, annotations) {

    var actionHandlers = {},

        getMessage = function(unannotated, annotated, quote) {
          var message;

          if (unannotated + annotated != 0) {
            if (annotated == 0) {
              if (unannotated == 1)
                message = 'There is 1 more <em>un-annotated</em> occurrence';
              else
                message = 'There are ' + unannotated + ' more <em>un-annotated</em> occurrences';
            } else if (unannotated == 0) {
              if (unannotated == 1)
                message = 'There is 1 other <em>annotated</em> occurrence';
              else
                message = 'There are ' + annotated + ' other <em>annotated</em> occurrences';
            } else {
              if (annotated == 1 && unannotated == 1)
                message = 'There is 1 more <em>un-annotated</em> and 1 <em>annotated</em> occurrence';
              else
                message = 'There are ' + unannotated + ' more <em>un-annotated</em> and ' + annotated + ' <em>annotated</em> occurrences';
            }

            message += ' of <span class="quote">' + quote + '</span> in the text. Do you want to re-apply this annotation?';
          }

          return message;
        },

        getButtons = function(annotated) {
          if (annotated > 0)
            return '<span class="stacked">' +
              '<button class="btn" data-action="REPLACE">YES, replace existing annotations</button>' +
              '<button class="btn" data-action="REPLACE">YES, merge with existing annotations</button>' +
              '<button class="btn outline" data-action="CANCEL">NO, don\'t re-apply</button>' +
            '</span>';
          else
            return '<span>' +
              '<button class="btn" data-action="REPLACE">YES</button>' +
              '<button class="btn outline" data-action="CANCEL">NO</button>' +
            '</span>';
        },

        onReplace = function(annotation, toReplace) {
          // Creates new annotations for un-annotated occurrences
          var selections = phraseAnnotator.createSelections(annotation);
          if (selections.length > 0 && actionHandlers.create)
            actionHandlers.create(selections);

          // For each annotation to replace, copy bodies from source annotation
          toReplace.forEach(function(replaced) {
            replaced.bodies = annotation.bodies.map(function(body) {
              return jQuery.extend({}, body);
            });
          });

          if (toReplace.length > 0 && actionHandlers.update)
            actionHandlers.update(toReplace);
        },

        onMerge = function(annotation, toReplace) {
          // TODO
          console.log('not yet implemented');
        },

        reapplyIfNeeded = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),

              unannotatedCount = phraseAnnotator.countOccurrences(quote),
              annotated = annotations.filterByQuote(quote).filter(function(a) {
                // Don't doublecount (or re-update) the current annotation
                return a.annotation_id != annotation.annotation_id;
              }),

              prompt = function() {
                var message = getMessage(unannotatedCount, annotated.length, quote),
                    buttons = getButtons(annotated.length),
                    element = jQuery(
                      '<div class="clicktrap">' +
                        '<div class="alert info reapply">' +
                          '<h1>Re-Apply</h1>' +
                          '<p class="message">' + message + '</p>' +
                          '<p class="buttons">' + buttons + '</div>' +
                        '</div>' +
                      '</div>').appendTo(document.body);

                element.on('click', 'button', function(evt) {
                  var btn = jQuery(evt.target),
                      action = btn.data('action');

                  if (action == 'REPLACE') onReplace(annotation, annotated);
                  if (action == 'MERGE') onMerge(annotation, annotated);

                  element.remove();
                });
              };

          if (unannotatedCount + annotated.length > 0) prompt();
        },

        on = function(evt, handler) {
          actionHandlers[evt] = handler;
        };

    this.reapplyIfNeeded = reapplyIfNeeded;
    this.on = on;
  };

  return Reapply;

});
