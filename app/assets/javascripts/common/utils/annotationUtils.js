define(function() {

  return {

    /** Attaches an annotation as payload to a DOM element **/
    bindToElements : function(annotation, elements) {
      var attach = function(element) {
            element.annotation = annotation;
            if (annotation.annotation_id)
              element.dataset.id = annotation.annotation_id;
          };

      if (jQuery.isArray(elements))
        jQuery.each(elements, function(idx, el) { attach(el); });
      else
        attach(elements);
    },

    /** Returns the number of comments on this annotation **/
    countComments : function(annotation) {
      return this.getBodiesOfType(annotation, 'COMMENT').length;
    },

    /** Deletes the given annotation body **/
    deleteBody : function(annotation, body) {
      var idx = annotation.bodies.indexOf(body);
      if (idx > -1)
        annotation.bodies.splice(idx, 1);
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

    /**
     * Returns the 'entity type' indicated by the annotation.
     *
     * Note: this assumes that annotations generally can't have two different entity
     * types. If they do, only the first in the list of bodies is returned.
     */
    getEntityType : function(annotation) {
      var t, i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        t = annotation.bodies[i].type;
        if (t === 'PLACE' || t === 'PERSON')
          return t;
      }
    },

    /**
     * Returns the value of the QUOTE body, if any.
     *
     * Note: this assumes that annotations generally only have a single QUOTE
     * body. If there are multiple, only the value of the first is returned.
     */
    getQuote : function(annotation) {
      var i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        if (annotation.bodies[i].type === 'QUOTE')
          return annotation.bodies[i].value;
      }
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

    /**
     * Tests if this annotation is empty.
     *
     * Annotations are considered empty if they have zero bodies, or only
     * one body of type QUOTE.
     */
    isEmpty : function(annotation) {
      if (annotation.bodies.length === 0)
        return true;
      else
        return annotation.bodies.length === 1 && annotation.bodies[0].type === 'QUOTE';
    },

    /** Sorts annotations by character offset **/
    sortByOffset : function(annotations) {
      return annotations.sort(function(a, b) {
        return a.anchor.substr(12) - b.anchor.substr(12);
      });
    },

    /** Sorts annotations by character offset (descending) **/
    sortByOffsetDesc : function(annotations) {
      return annotations.sort(function(a, b) {
        return b.anchor.substr(12) - a.anchor.substr(12);
      });
    }

  };

});
