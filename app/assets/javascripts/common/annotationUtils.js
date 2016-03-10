define(function() {

  var AnnotationUtils = {

    getQuote: function(annotation) {
      var i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        if (annotation.bodies[i].type === 'QUOTE')
          return annotation.bodies[i].value;
      }
    },

    /** Note: this assumes that annotations can't have two different entity types! **/
    getEntityType: function(annotation) {
      var t, i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        t = annotation.bodies[i].type;
        if (t === 'PLACE' || t === 'PERSON')
          return t;
      }
    },

    sortByOffset: function(annotations) {
      return annotations.sort(function(a, b) {
        var offsetA = a.anchor.substr(12),
            offsetB = b.anchor.substr(12);

        return offsetA - offsetB;
      });
    }

  };

  return AnnotationUtils;

});
