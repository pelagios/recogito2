define([
  'common/utils/annotationUtils'
], function(Utils) {

  var Annotations = function() {

    var annotations =  [],

        uniqueTags = [],

        add = function(annotations) {
          // TODO just a quick hack to parse unique tags
          var arr = (jQuery.isArray(annotations)) ? annotations : [ annotations ];
          arr.forEach(function(a) {
            var tags = Utils.getBodiesOfType(a, 'TAG').map(function(t) {
                  return t.value;
                });

            tags.forEach(function(tag) {
              if (uniqueTags.indexOf(tag) < 0)
                uniqueTags.push(tag);
            });
          });
        },

        remove = function(annotation) {

        },

        getReadOnlyView = function() {
          return readOnlyView;
        },

        getUniqueTags = function() {
          return uniqueTags;
        },

        getAnnotations = function(filter) {

        };

    this.add = add;
    this.remove = remove;

    // This way we can hand out a reference to the read-access
    // methods to other UI components, while preventing write access
    this.readOnly = function() {
      return {
        getUniqueTags: getUniqueTags,
        getAnnotation: getAnnotations
      };
    };
  };

  return Annotations;

});
