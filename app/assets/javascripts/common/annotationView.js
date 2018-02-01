define([
  'common/utils/annotationUtils'
], function(Utils) {

  var Annotations = function(initial) {

    var annotations = (initial) ? initial : [],

        uniqueTags = [],

        contributors = [],

        isEmpty = function(maybeArr) {
          if (maybeArr && maybeArr.length > 0) return false;
          else return true;
        },

        collectInto = function(fn, arr) {
          annotations.forEach(function(a) {
            fn(a).forEach(function(value) {
              if (arr.indexOf(value) < 0)
                arr.push(value);
            });
          });
        },

        set = function(a) {
          annotations = (jQuery.isArray(a)) ? a : [ a ];
          // Reset unique tags array (will be rebuilt on next access)
          uniqueTags = [];
        },

        add = function() {
          // TODO implement
        },

        remove = function() {
          // TODO implement
        },

        getFiltered = function(filter) {
          return annotation.filter(filter);
        },

        listUniqueTags = function() {
          var getTags = function(a) { return Utils.getTags(a); };
          if (isEmpty(uniqueTags))
            collectInto(getTags, uniqueTags);
          return uniqueTags;
        },

        listContributors = function() {
          var getContributors = function(a) { return a.contributors; };
          if (isEmpty(contributors))
            collectInto(getContributors, contributors);
          return contributors;
        },

        listAll = function() {
          return annotations;
        };

    this.add = add;
    this.remove = remove;
    this.set = set;

    // This way we can hand out a 'read-only' reference to other UI components.
    // Note that the annotation array as such is mutable, so we can't actually prevent
    // write access. But returning new copies every time seems too costly.
    this.readOnly = function() {
      return {
        getFiltered      : getFiltered,
        listContributors : listContributors,
        listUniqueTags   : listUniqueTags,
        listAll          : listAll
      };
    };
  };

  return Annotations;

});
