define([], function() {

  var Annotations = function() {

    var annotations =  [],

        uniqueTags = [],

        add = function(annotations) {
          console.log('Adding ' + annotations.length + ' annotations');
        },

        remove = function(annotation) {

        },

        getReadOnlyView = function() {
          return readOnlyView;
        },

        getUniqueTags = function() {
          return ['my', 'tags', 'foobar'];
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
