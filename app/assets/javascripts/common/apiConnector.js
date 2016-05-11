define([], function() {

  return {

    loadAnnotations : function(documentId, partNumber) {
      return jsRoutes.controllers.api.AnnotationAPIController.loadAnnotations().ajax({
        data: {
          doc: documentId,
          part: partNumber
        }
      });
    },

    storeAnnotation : function(annotation) {
      return jsRoutes.controllers.api.AnnotationAPIController.createAnnotation().ajax({
        type: 'POST',
        data: JSON.stringify(annotation),
        contentType: 'application/json'
      });
    },

    deleteAnnotation : function(id) {
      return jsRoutes.controllers.api.AnnotationAPIController.deleteAnnotation(id).ajax();
    }

  };

});
