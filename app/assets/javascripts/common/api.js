define([], function() {

  return {

    /** Annotation API **/

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

    /** Place API **/

    getPlacesInDocument : function(docId, offset, size) {
      var o = (offset) ? offset : 0,
          s = (size) ? size : 20;

      return jsRoutes.controllers.api.PlaceAPIController.listPlacesInDocument(docId, o, s).ajax();
    },

    searchPlaces : function(query, offset, size) {
      var o = (offset) ? offset : 0,
          s = (size) ? size : 20;

      return jsRoutes.controllers.api.PlaceAPIController.searchPlaces(query, o, s).ajax();
    }

  };

});
