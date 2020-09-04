define([
  'common/config',
  'common/utils/annotationUtils'
], function(Config, Utils) {

  var Annotations = function(initial) {

    var annotations = (initial) ? initial : [],

        contributors = [],

        uniqueTags = [],

        /** Checks if an array is undefined or empty **/
        isEmpty = function(maybeArr) {
          if (maybeArr && maybeArr.length > 0) return false;
          else return true;
        },

        /** Shorthand to add to an array of distinct elements, since we're lacking a decent set **/
        addIfNotExists = function(element, array) {
          if (array.indexOf(element) < 0)
           array.push(element);
        },

        /**
         * Utility function to collect values into an array of distinct elements.
         *
         * Implements the functionality common to building the uniqueTags, contributor etc. sets.
         */
        collectIntoIfEmpty = function(fn, arr) {
          if (isEmpty(arr))
            annotations.forEach(function(a) {
              fn(a).forEach(function(value) {
                addIfNotExists(value, arr);
              });
            });
        },

        /** Resets the derived sets (uniqueTags, contributors etc.) **/
        resetDerivedSets = function(annotations) {
          contributors = [];

          var tags = annotations.reduce(function(tags, annotation) {
            return tags.concat(Utils.getTags(annotation));
          }, []);

          var uniqueLabels = uniqueTags.map(function(t) { return t.value || t; });

          tags.forEach(function(t) {
            if (uniqueLabels.indexOf(t) == -1) {
              uniqueTags.push(t);
            }
          });
        },

        /** Add an annotation or array of annotations to the view **/
        add = function(a) {
          var newAnnotations = (jQuery.isArray(a)) ? a : [ a ];
          annotations = annotations.concat(newAnnotations);

          // This could be optimized. We could just add to the existing.
          // But before that, we'd need to check if empty (i.e. not a one-liner, and
          // performance increase might be minimal unless for really long lists of
          // annotations
          resetDerivedSets(newAnnotations);
        },

        addOrReplace = function(a) {
          var newAnnotations = (jQuery.isArray(a)) ? a : [ a ],

              addOrReplaceOne = function(annotation) {
                var existing = annotations.find(function(a) {
                      return a.annotation_id === annotation.annotation_id;
                    }),

                    idx = (existing) ? annotations.indexOf(existing) : -1;

                if (idx === -1)
                  annotations.push(annotation);
                else
                  annotations[idx] = annotation;
              };

          newAnnotations.forEach(addOrReplaceOne);
          resetDerivedSets(newAnnotations);
        },

        /** Removes an annotation or array of annotations to the view **/
        remove = function(a) {
          var toRemove = (jQuery.isArray(a)) ? a : [ a ],

              removeOne = function(one) {
                var idx = annotations.indexOf(one);
                if (idx > -1)
                  annotations.splice(idx, 1);
              };
              
          toRemove.forEach(removeOne);
          resetDerivedSets([]);
        },

        /** Filters annotations by any filter function **/
        filter = function(filter) {
          return annotations.filter(filter);
        },

        forEach = function(fn) {
          annotations.forEach(fn);
        },

        /** Filters annotations to those that have the specified body type **/
        filterByBodyType = function(bodyType) {
          return annotations.filter(function(a) {
            var bodiesOfType = Utils.getBodiesOfType(a, bodyType);
            return bodiesOfType.length > 0;
          });
        },

        /** Filters annotations to those with a specific quote body value **/
        filterByQuote = function(quote) {
          return annotations.filter(function(a) {
            var quoteBodies = Utils.getBodiesOfType(a, 'QUOTE');
            // Safe to assume annotations have only a single quote body
            return quoteBodies.length > 0 && quoteBodies[0].value === quote;
          });
        },

        /**
         * Filters annotations to those with a specific entity URI.
         * Note that we'll need to adapt this method once we get more
         * than just PLACE entity types.
         */
        filterByEntityURI = function(uri) {
          return annotations.filter(function(a) {
            // TODO needs extending once we have more than just places
            var entityBodies = Utils.getBodiesOfType(a, 'PLACE'),
                entityURIs = [];

            entityBodies.forEach(function(b) {
              if (b.uri) entityURIs.push(b);
            });

            return entityBodies.indexOf(uri) > -1;
          });
        },

        listContributors = function() {
          var getContributors = function(a) { return a.contributors; };
          collectIntoIfEmpty(getContributors, contributors);
          return contributors;
        },

        listUniqueTags = function() {
          return uniqueTags;
        },

        listAnnotations = function() {
          return annotations;
        },

        fetchTags = function() {
          // Previously used tags
          jsRoutes.controllers.document.stats.StatsController.getTagsAsJSON(Config.documentId)
            .ajax().then(function(response) {
              // Tags used previously on any part of this document
              // HACK - these are always labels, not objects ({ value, uri })
              var previouslyUsedTags = response.map(function(tag) { return tag.value });

              // Unique tags = controlled vocabulary (if any) + previously used tags
              // Make sure the controlled vocab comes first, and the order isn't changed
              // HACK - vocabulary can be list of strings or list of objects
              uniqueTags = Config.vocabulary || [];
              
              var vocabLabels = uniqueTags.map(function(strOrObj) {
                return strOrObj.value || strOrObj; 
              });

              previouslyUsedTags.forEach(function(tag) {
                if (vocabLabels.indexOf(tag) === -1)
                  uniqueTags.push(tag);
              });
            });
        };

    fetchTags();

    this.add = add;
    this.addOrReplace = addOrReplace;
    this.remove = remove;

    // This way we can hand out a 'read-only' reference to other UI components.
    // Note that the annotation array as such is mutable, so we can't actually prevent
    // write access. But returning new copies every time seems too costly.
    this.readOnly = function() {
      return {
        filter           : filter,
        forEach          : forEach,
        filterByBodyType : filterByBodyType,
        filterByQuote    : filterByQuote,
        listContributors : listContributors,
        listUniqueTags   : listUniqueTags,
        listAnnotations  : listAnnotations
      };
    };
  };

  return Annotations;

});
