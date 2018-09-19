define([
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/annotation/common/selection/reapply/annotate/modal'
], function(AnnotationUtils, PlaceUtils, Modal) {

  var ReAnnotate = function(phraseAnnotator, annotations) {

    var actionHandlers = {},

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
                      original.bodies.filter(function(b) {
                        var isPlace = b.type == 'PLACE',
                            isIdentical = AnnotationUtils.containsBodyOfValue(newPlaceBodies, b),

                            // Remove all place bodies from the original, unless
                            // they are identical to one in the list of new
                            // place bodies.
                            toRemove = isPlace && !isIdentical;

                        return !toRemove; // false means 'remove'
                      }) :
                      original.bodies,

                    bodiesToAppend = annotation.bodies.filter(function(b) {
                      // Don't append bodies which the original already has
                      return !AnnotationUtils.containsBodyOfValue(original, b);
                    });

                original.bodies = bodiesToKeep.concat(bodiesToAppend);
              };

          reapplyToUnannotated(annotation); // Nothing to merge here
          toReplace.forEach(mergeOne);

          if (toReplace.length > 0 && actionHandlers.update)
            actionHandlers.update(toReplace);
        },

        onAdvanced = function(annotation, unannotatedCount) {
          var bulkEl = document.getElementById('bulk-annotation'),
              evt = new Event('open'),

              okListener = function(evt) {
                console.log('Received answer:');
                console.log(evt.args);
                bulkEl.removeEventListener('ok', okListener, false);
              }

          evt.args = {
            mode: 'REAPPLY',
            original: annotation,
            annotations: annotations.listAnnotations(),
            unannotatedMatches: unannotatedCount,
            uriParser:PlaceUtils.parseURI
          };

          bulkEl.dispatchEvent(evt);
          bulkEl.addEventListener('ok', okListener);
        },

        reapplyIfNeeded = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),

              unannotatedCount = phraseAnnotator.countOccurrences(quote),
              annotated = annotations.filterByQuote(quote).filter(function(a) {
                // Don't doublecount (or re-update) the current annotation
                return a.annotation_id != annotation.annotation_id;
              });

          if (unannotatedCount + annotated.length > 0)
            Modal.prompt(quote, unannotatedCount, annotated, {
              'MERGE': onMerge.bind(this, annotation, annotated),
              'REPLACE': onReplace.bind(this, annotation, annotated),
              'ADVANCED': onAdvanced.bind(this, annotation, unannotatedCount)
            });
        };

        on = function(evt, handler) {
          actionHandlers[evt] = handler;
        };

    this.reapplyIfNeeded = reapplyIfNeeded;
    this.on = on;
  };

  return ReAnnotate;

});
