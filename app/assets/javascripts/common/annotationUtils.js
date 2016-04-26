define(function() {

  var AnnotationUtils = {

    /** Attaches an annotation as payload to a DOM element **/
    attachAnnotation: function(element, annotation) {
      element.annotation = annotation;
      if (annotation.annotation_id)
        element.dataset.id = annotation.annotation_id;
    },

    /** Returns the value of the QUOTE body, if any **/
    getQuote: function(annotation) {
      var i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        if (annotation.bodies[i].type === 'QUOTE')
          return annotation.bodies[i].value;
      }
    },

    /** Returns the bodies of the specified type as an array **/
    getBodiesOfType: function(annotation, type) {
      var t, i, len, bodies = [];

      for (i = 0, len = annotation.bodies.length; i < len; i++) {
        t = annotation.bodies[i].type;
        if (t === type)
          bodies.push(annotation.bodies[i]);
      }

      return bodies;
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
