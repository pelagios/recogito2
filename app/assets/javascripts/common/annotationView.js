define([
  'common/utils/annotationUtils'
], function(Utils) {

  var isEmpty = function(maybeArr) {
    if (maybeArr && maybeArr.length > 0) return false;
    else return true;
  };

  var Annotations = function(initial) {

    var annotations = (initial) ? initial : [],

        uniqueTags = [],

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
          var buildUniqueTags = function() {
                uniqueTags = [];

                annotations.forEach(function(a) {
                  Utils.getTags(a).forEach(function(tag) {
                    if (uniqueTags.indexOf(tag) < 0)
                      uniqueTags.push(tag);
                  });
                });
              };

          if (isEmpty(uniqueTags))
            buildUniqueTags();

          return uniqueTags;
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
        getFiltered: getFiltered,
        listUniqueTags: listUniqueTags,
        listAll : listAll
      };
    };
  };

  return Annotations;

});
