define([], function() {

  return {

    /** Annotation API **/

    getAnnotation : function(uuid, opt_include_context) {
      var context = (opt_include_context) ? opt_include_context : false;
      return jsRoutes.controllers.api.AnnotationAPIController.getAnnotation(uuid, context).ajax();
    },

    listAnnotationsInDocument : function(docId) {
      return jsRoutes.controllers.api.AnnotationAPIController.listAnnotationsInDocument(docId).ajax();
    },

    listAnnotationsInPart : function(docId, partNo) {
      return jsRoutes.controllers.api.AnnotationAPIController.listAnnotationsInPart(docId, partNo).ajax();
    },

    storeAnnotation : function(annotation) {
      return jsRoutes.controllers.api.AnnotationAPIController.createAnnotation().ajax({
        type: 'POST',
        data: JSON.stringify(annotation),
        contentType: 'application/json'
      });
    },

    bulkUpsertAnnotations : function(annotations) {
      return jsRoutes.controllers.api.AnnotationAPIController.bulkUpsert().ajax({
        type: 'POST',
        data: JSON.stringify(annotations),
        contentType: 'application/json'
      });
    },

    deleteAnnotation : function(id) {
      return jsRoutes.controllers.api.AnnotationAPIController.deleteAnnotation(id).ajax();
    },

    /** Place API **/

    getPlace : function(uri) {
      return jsRoutes.controllers.api.PlaceAPIController.findPlaceByURI(uri).ajax();
    },

    listPlacesInDocument : function(docId, offset, size) {
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
