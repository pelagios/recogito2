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
                message = 'There is 1 more un-annotated occurrence';
              else
                message = 'There are ' + unannotated + ' more un-annotated occurrences';
            } else if (unannotated == 0) {
              if (unannotated == 1)
                message = 'There is 1 other annotated occurrence';
              else
                message = 'There are ' + annotated + ' other annotated occurrences';
            } else {
              if (annotated == 1 && unannotated == 1)
                message = 'There is 1 <em>un-annotated</em> and 1 <em>annotated</em> occurrence';
              else
                message = 'There are ' + unannotated + ' <em>un-annotated</em> and ' + annotated + ' <em>annotated</em> occurrences';
            }

            message += ' of <span class="quote">' + quote + '</span> in the text. Do you want to re-apply this annotation?';
          }

          return message;
        },

        getButtons = function(annotated) {
          if (annotated > 0)
            return '<span class="stacked">' +
              '<button class="btn" data-action="REPLACE">YES, replace existing annotations</button>' +
              '<button class="btn" data-action="MERGE">YES, merge with existing annotations</button>' +
              '<button class="btn outline" data-action="CANCEL">NO, don\'t re-apply</button>' +
            '</span>';
          else
            return '<span>' +
              '<button class="btn" data-action="REPLACE">YES</button>' +
              '<button class="btn outline" data-action="CANCEL">NO</button>' +
            '</span>';
        },

        /** Re-applies the annotation to the un-annotated text **/
        reapplyToUnannotated = function(annotation) {
          var selections = phraseAnnotator.createSelections(annotation);
          if (selections.length > 0 && actionHandlers.create)
            actionHandlers.create(selections);
        }

        onReplace = function(annotation, toReplace) {
          reapplyToUnannotated(annotation); // Nothing to replace here

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
          // By convention, "merge" replaces the current place bodies,
          // and appends all other bodies
          var newPlaceBodies = AnnotationUtils.getBodiesOfType(annotation, 'PLACE'),

              mergeOne = function(original) {
                var bodiesToKeep = (newPlaceBodies.length > 0) ?
                      original.bodies.filter(function(b) { return b.type != 'PLACE' }) :
                      original.bodies,

                    bodiesToAppend = annotation.bodies.filter(function(b) {
                      // Don't append bodies which the original already has
                      return !AnnotationUtils.containsBodyOfValue(bodiesToKeep, b);
                    });

                original.bodies = bodiesToKeep.concat(bodiesToAppend);
              };

          reapplyToUnannotated(annotation); // Nothing to merge here
          toReplace.forEach(mergeOne);

          if (toReplace.length > 0 && actionHandlers.update)
            actionHandlers.update(toReplace);
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
