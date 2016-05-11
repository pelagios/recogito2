define([], function() {

  return {

    /** Annotation API methods **/

    getAnnotationsForDocument : function(docId) {
      return jsRoutes.controllers.api.AnnotationAPIController.getAnnotationsForDocument(docId).ajax();
    },

    getAnnotationsForPart : function(docId, partNo) {
      return jsRoutes.controllers.api.AnnotationAPIController.getAnnotationsForPart(docId, partNo).ajax();
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
    },

    /** Place API methods **/

    getPlacesInDocument : function(docId) {
      return jsRoutes.controllers.api.PlaceAPIController.getPlacesInDocument(docId).ajax();
    },

    searchPlaces : function(query) {
      return jsRoutes.controllers.api.PlaceAPIController.searchPlaces(query).ajax();
    }

  };

});
