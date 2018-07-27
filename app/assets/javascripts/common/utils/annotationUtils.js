define(function() {

  return {

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
     * Returns true if bodies are equal in terms of their value. I.e.
     * this check considers type, value and URI fields, but ignores
     * creator and creation timestamp.
     */
    bodyValueEquals : function(a, b) {
      if (a.type != b.type) return false;
      if (a.value != b.value) return false;
      if (a.uri != b.uri) return false;
      return true;
    },

    /**
     * Returns true if the annotation contains a body with the same
     * value as the given body. Equality is checked according to the rules
     * of bodyValueEquals (above)
     */
    containsBodyOfValue : function(annotation, body) {
      var that = this;
      return annotation.bodies.find(function(b) {
        return that.bodyValueEquals(b, body);
      });
    },

    /** Shorthand **/
    getTags : function(annotation) {
      return this.getBodiesOfType(annotation, 'TAG').map(function(t) {
        return t.value;
      });
    },

    /**
     * Returns the first 'entity body' found in the annotation,
     * i.e. the first body that's of PLACE, PERSON or EVENT type.
     */
    getFirstEntity : function(annotation) {
      var t, i, len;
      for (i=0, len=annotation.bodies.length; i<len; i++) {
        t = annotation.bodies[i].type;
        if (t === 'PLACE' || t === 'PERSON' || t === 'EVENT')
          return annotation.bodies[i];
      }
    },

    /**
     * Returns the 'entity type' indicated by the annotation.
     *
     * Note: this assumes that annotations generally can't have two different entity
     * types. If they do, only the first in the list of bodies is returned.
     */
    getEntityType : function(annotation) {
      var firstEntity = this.getFirstEntity(annotation);
      if (firstEntity)
        return firstEntity.type;
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

    /** Returns an array with the values of the transcription bodies **/
    getTranscriptions : function(annotation) {
      var transcriptions = [];

      jQuery.each(annotation.bodies, function(idx, body) {
        if (body.type === 'TRANSCRIPTION')
          transcriptions.push(body.value);
      });

      return transcriptions;
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
