define(function() {

  var AnnotationUtils = {

    /** Tests if this annotation is empty **/
    isEmpty : function(annotation) {
      if (annotation.bodies.length === 0)
        // Yep, definitely empty
        return true;
      else
        // If all that's left is a QUOTE body, consider it empty, too
        return annotation.bodies.length === 1 && annotation.bodies[0].type === 'QUOTE';
    },

    /**
     * Returns status info for this annotation.
     *
     * Remember that 'status' is a property of (some types of) bodies, rather
     * than the annotation as a whole. This method returns a list of all distinct
     * status values found in the bodies of this annotation.
     */
    getStatus : function(annotation) {
      var statusValues = [];

      jQuery.each(annotation.bodies, function(idx, body) {
        if (body.status && statusValues.indexOf(body.status.value) < 0)
            statusValues.push(body.status.value);
      });

      return statusValues;
    },

    /** Attaches an annotation as payload to a DOM element **/
    attachAnnotation : function(element, annotation) {
      var attach = function(element) {
            element.annotation = annotation;
            if (annotation.annotation_id)
              element.dataset.id = annotation.annotation_id;
          };

      if (jQuery.isArray(element))
        jQuery.each(element, function(idx, el) { attach(el); });
      else
        attach(element);
    },

    /** Returns the value of the QUOTE body, if any **/
    getQuote : function(annotation) {
      var i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        if (annotation.bodies[i].type === 'QUOTE')
          return annotation.bodies[i].value;
      }
    },

    /** Returns the bodies of the specified type as an array **/
    getBodiesOfType : function(annotation, type) {
      var t, i, len, bodies = [];

      for (i = 0, len = annotation.bodies.length; i < len; i++) {
        t = annotation.bodies[i].type;
        if (t === type)
          bodies.push(annotation.bodies[i]);
      }

      return bodies;
    },

    /** Note: this assumes that annotations can't have two different entity types! **/
    getEntityType : function(annotation) {
      var t, i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        t = annotation.bodies[i].type;
        if (t === 'PLACE' || t === 'PERSON')
          return t;
      }
    },

    sortByOffset : function(annotations) {
      return annotations.sort(function(a, b) {
        var offsetA = a.anchor.substr(12),
            offsetB = b.anchor.substr(12);

        return offsetB - offsetA;
      });
    },

    deleteBody : function(annotation, body) {
      var idx = annotation.bodies.indexOf(body);
      if (idx > -1)
        annotation.bodies.splice(idx, 1);
    },

    countComments : function(annotation) {
      return this.getBodiesOfType(annotation, 'COMMENT').length;
    }

  };

  return AnnotationUtils;

});
