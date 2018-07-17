define([
  'common/utils/annotationUtils'
], function(Utils) {

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
        resetDerivedSets = function() {
          contributors = [];
          uniqueTags = [];
        },

        /** Add an annotation or array of annotations to the view **/
        add = function(a) {
          var newAnnotations = (jQuery.isArray(a)) ? a : [ a ];
          annotations = annotations.concat(newAnnotations);

          // This could be optimized. We could just add to the existing.
          // But before that, we'd need to check if empty (i.e. not a one-liner, and
          // performance increase might be minimal unless for really long lists of
          // annotations
          resetDerivedSets();
        },

        /** Removes an annotatoin or array of annotations to the view **/
        remove = function(a) {
          var idx = annotations.indexOf(a);
          if (idx > -1) {
            annotations.splice(idx, 1);
            resetDerivedSets();
          }
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

        listContributors = function() {
          var getContributors = function(a) { return a.contributors; };
          collectIntoIfEmpty(getContributors, contributors);
          return contributors;
        },

        listUniqueTags = function() {
          var getTags = function(a) { return Utils.getTags(a); };
          collectIntoIfEmpty(getTags, uniqueTags);
          return uniqueTags;
        },

        listAnnotations = function() {
          return annotations;
        };

    this.add = add;
    this.remove = remove;

    // This way we can hand out a 'read-only' reference to other UI components.
    // Note that the annotation array as such is mutable, so we can't actually prevent
    // write access. But returning new copies every time seems too costly.
    this.readOnly = function() {
      return {
        filter           : filter,
        forEach          : forEach,
        filterByBodyType : filterByBodyType,
        listContributors : listContributors,
        listUniqueTags   : listUniqueTags,
        listAnnotations  : listAnnotations
      };
    };
  };

  return Annotations;

});
