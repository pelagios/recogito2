define([
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/annotation/common/selection/reapply/annotate/modal'
], function(AnnotationUtils, PlaceUtils, Modal) {

  var ReAnnotate = function(phraseAnnotator, annotations) {

    var actionHandlers = {},

        /**
         * Helper to check if a list of bodies contains the
         * given body. NOTE: this method uses
         * AnnotationUtils.bodyValueEquals(), which checks
         * for equality based only on type/value/URI, ignoring
         * creator, timestamp (or object equality).
         */
        containsBody = function(list, body) {
          return list.find(function(b) {
            return AnnotationUtils.bodyValueEquals(b, body);
          });
        },

        /**
         * Helper function that compares bodiesToFilter with refBodies,
         * and removes bodies that exist in refBodies from bodiesToFilter.
         * We're using this to make sure APPEND operations don't repeat
         * annotation bodies that already exist in the original annotation.
         */
        removeBodies = function(refBodies, bodiesToFilter) {
          return bodiesToFilter.filter(function(b) {
            return !containsBody(refBodies, b);
          });
        },

        /**
         * Helper function to find bodies that appear in both lists.
         */
        intersectBodies = function(bodiesA, bodiesB) {
          var intersection = [];

          bodiesA.forEach(function(b) {
            if (containsBody(bodiesB, b))
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
        reapplyToUnannotated = function(annotation) {
          var selections = phraseAnnotator.createSelections(annotation);
          if (selections.length > 0 && actionHandlers.create)
            actionHandlers.create(selections);
        },

        /**
         * Re-applies the annotation to annotated matches, using
         * REPLACE as a merge strategy. Optionally filters by verification
         * status.
         *
         * Will take into account admin status and potential privilege
         * conflicts. (They will be re-checked at the server-side anyway. But
         * by checking on the client, we avoid out-of-sync conflicts upfront.)
         */
        replaceAnnotated = function(annotation, toApply) {
          var isAllowed = function(annotation) {
                // TODO implement
                return true;
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
         * APPEND as a merge strategy. Optionally filters by annotation status.
         */
        appendToAnnotated = function(annotation, toApply) {
          toApply.forEach(function(applied) {
            var origBodies = applied.bodies,
                bodiesToAppend = clone(removeBodies(origBodies, annotation.bodies));
                updatedBodies = origBodies.concat(bodiesToAppend);
            applied.bodies = updatedBodies;
          });
        },

        /**
         * Re-applies the annotation to annotated matches, using
         * MIXED MERGE as a merge strategy. Optionally filters by annotation
         * status.
         */
        mergeWithAnnotated = function(annotation, toApply) {

          // TODO implement

        },

        /*
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
        */

        /** Triggered from the first (non-advanced) panel **/
        onQuickMerge = function(annotation, toReplace) {
          if (toReplace.length > 0) {
            reapplyToUnannotated(annotation);
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
            return true; // TODO implement
          }) : filtered;
        },

        executeAdvanced = function(annotation, args) {
          var toApply = (args.applyToAnnotated) ?
            getAnnotationsToModify(annotation, args.applyIfStatus) : false ;

          if (args.applyToUnannotated)
            reapplyToUnannotated(annotation);

          if (args.applyToAnnotated) {
            if (args.mergePolicy == 'APPEND')
              appendToAnnotated(annotation, toApply);

            else if (args.mergePolicy == 'REPLACE')
              replaceAnnotated(annotation, toApply);

            else if (args.mergePolicy == 'MIXED') {}
              mergeWithAnnotated(annotation, toApply);
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
            unannotatedMatches: unannotatedCount,
            uriParser:PlaceUtils.parseURI
          };

          bulkEl.dispatchEvent(evt);
          bulkEl.addEventListener('ok', okListener);
        },

        reapplyIfNeeded = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),
              unannotatedCount = phraseAnnotator.countOccurrences(quote),
              annotated = getAnnotationsToModify(annotation);

          if (unannotatedCount + annotated.length > 0)
            Modal.prompt(quote, unannotatedCount, annotated, {
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
