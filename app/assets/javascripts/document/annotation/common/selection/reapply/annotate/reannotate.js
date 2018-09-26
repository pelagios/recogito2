define([
  'common/config',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/annotation/common/selection/reapply/annotate/modal'
], function(Config, AnnotationUtils, PlaceUtils, Modal) {

  var ReAnnotate = function(phraseAnnotator, annotations) {

    var actionHandlers = {},

        /**
         * Helper function that compares bodiesToFilter with refBodies,
         * and removes bodies that exist in refBodies from bodiesToFilter.
         * We're using this to make sure APPEND operations don't repeat
         * annotation bodies that already exist in the original annotation.
         */
        removeBodies = function(refBodies, bodiesToFilter) {
          return bodiesToFilter.filter(function(b) {
            return !AnnotationUtils.containsBodyOfValue(refBodies, b);
          });
        },

        /**
         * Helper function to find bodies that appear in both lists.
         */
        intersectBodies = function(bodiesA, bodiesB) {
          var intersection = [];

          bodiesA.forEach(function(b) {
            if (AnnotationUtils.containsBodyOfValue(bodiesB, b))
              intersection.push(b);
          });

          return intersection;
        },

        clone = function(bodies) {
          return bodies.map(function(body) {
            return jQuery.extend({}, body);
          });
        },

        /**
         * Re-applies the annotation to the un-annotated text
         */
        reapplyToUnannotated = function(annotation, matchType) {
          var requireFullWord = matchType == 'FULL_WORD',
              selections = phraseAnnotator.createSelections(annotation, requireFullWord);

          if (selections.length > 0 && actionHandlers.create)
            actionHandlers.create(selections);
        },

        /**
         * Re-applies the annotation to annotated matches, using
         * REPLACE as a merge strategy.
         *
         * Will take into account admin status and potential privilege
         * conflicts. (They will be re-checked at the server-side anyway. But
         * by checking on the client, we avoid out-of-sync conflicts upfront.)
         */
        replaceAnnotated = function(annotation, toApply) {
          var isAllowed = function(annotation) {
                if (Config.isAdmin)
                  return true;

                var commentsByOthers =
                       AnnotationUtils.getBodiesOfType(annotation, 'COMMENT')
                         .filter(function(b) {
                           return b.last_modified_by != Config.me;
                       });

                return commentsByOthers.length == 0;
              };

          toApply.forEach(function(applied) {
            if (isAllowed(applied)) {
              var origBodies = applied.bodies,
                  bodiesToKeep = intersectBodies(origBodies, annotation.bodies);
                  bodiesToAppend = clone(removeBodies(bodiesToKeep, annotation.bodies));
                  updatedBodies = bodiesToKeep.concat(bodiesToAppend);
              applied.bodies = updatedBodies;
            }
          });
        },

        /**
         * Re-applies the annotation to annotated matches, using
         * APPEND as a merge strategy.
         */
        appendToAnnotated = function(annotation, toApply) {
          toApply.forEach(function(applied) {
            var origBodies = applied.bodies,
                bodiesToAppend = removeBodies(origBodies, clone(annotation.bodies));
                updatedBodies = origBodies.concat(bodiesToAppend);

            applied.bodies = updatedBodies;
          });
        },

        /**
         * Re-applies the annotation to annotated matches, using
         * MIXED MERGE as a merge strategy.
         */
        mergeWithAnnotated = function(annotation, toApply) {
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

                    bodiesToAppend = clone(annotation.bodies.filter(function(b) {
                      // Don't append bodies which the original already has
                      return !AnnotationUtils.containsBodyOfValue(original, b);
                    }));

                original.bodies = bodiesToKeep.concat(bodiesToAppend);
              };

          toApply.forEach(mergeOne);
        },

        /** Triggered from the first (non-advanced) panel **/
        onQuickMerge = function(annotation, toReplace) {
          if (toReplace.length > 0) {
            reapplyToUnannotated(annotation, 'FULL_WORD');

            mergeWithAnnotated(annotation, toReplace);
            if (actionHandlers.update)
              actionHandlers.update(toReplace);
          }
        },

        getAnnotationsToModify = function(original, opt_status) {
          var quote = AnnotationUtils.getQuote(original),
              filtered = annotations.filterByQuote(quote).filter(function(a) {
                // Don't double-count (or re-update) the original annotation
                return a.annotation_id != original.annotation_id;
              });

          return (opt_status) ? filtered.filter(function(a) {
            var statusValues = AnnotationUtils.getStatus(a);
            // Note: we're only evaluating the first status
            return (statusValues.length > 0) && statusValues[0] == opt_status;
          }) : filtered;
        },

        executeAdvanced = function(annotation, args) {
          var toApply = (args.applyToAnnotated) ?
            getAnnotationsToModify(annotation, args.applyIfStatus) : false ;

          if (args.applyToUnannotated && !args.applyIfStatus)
            reapplyToUnannotated(annotation, args.applyIfMatchType);

          if (args.applyToAnnotated) {
            if (args.mergePolicy == 'APPEND')
              appendToAnnotated(annotation, toApply);
            else if (args.mergePolicy == 'REPLACE')
              replaceAnnotated(annotation, toApply);
            else if (args.mergePolicy == 'MIXED')
              mergeWithAnnotated(annotation, toApply);

            if (actionHandlers.update && toApply.length > 0)
              actionHandlers.update(toApply);
          }
        },

        onGoAdvanced = function(annotation, unannotatedCount) {
          var bulkEl = document.getElementById('bulk-annotation'),
              evt = new Event('open'),

              okListener = function(evt) {
                executeAdvanced(annotation, evt.args);
                bulkEl.removeEventListener('ok', okListener, false);
              }

          evt.args = {
            mode: 'REAPPLY',
            original: annotation,
            annotations: annotations.listAnnotations(),
            uriParser: PlaceUtils.parseURI,
            phraseCounter: phraseAnnotator.countOccurrences
          };

          bulkEl.dispatchEvent(evt);
          bulkEl.addEventListener('ok', okListener);
        },

        reapplyIfNeeded = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),
              // Require full-word match as default
              unannotatedCount = phraseAnnotator.countOccurrences(quote, true),
              annotated = getAnnotationsToModify(annotation);

          if (unannotatedCount + annotated.length > 0)
            Modal.prompt(quote, unannotatedCount, annotated, {
              'UNANNOTATED': reapplyToUnannotated.bind(this, annotation, 'FULL_WORD'),
              'MERGE': onQuickMerge.bind(this, annotation, annotated),
              'ADVANCED': onGoAdvanced.bind(this, annotation, unannotatedCount)
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
