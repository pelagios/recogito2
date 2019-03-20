define([
  'common/utils/placeUtils',
  'common/config'
], function(PlaceUtils, Config) {

  return {

    /** Annotation API **/

    getAnnotation : function(uuid, opt_include_context) {
      var context = (opt_include_context) ? opt_include_context : false;
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.getAnnotation(uuid, context).ajax();
    },

    listAnnotationsInDocument : function(docId) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.listAnnotationsInDocument(docId).ajax();
    },

    listAnnotationsInPart : function(docId, partNo) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.listAnnotationsInPart(docId, partNo).ajax();
    },

    storeAnnotation : function(annotation) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.createAnnotation().ajax({
        type: 'POST',
        data: JSON.stringify(annotation),
        contentType: 'application/json'
      }).then(function(result) {
        console.log(result);
      });
    },

    storeAnnotationBatch : function(annotations) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.bulkUpsert().ajax({
        type: 'POST',
        data: JSON.stringify(annotations),
        contentType: 'application/json'
      });
    },

    bulkUpsertAnnotations : function(annotations) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.bulkUpsert().ajax({
        type: 'POST',
        data: JSON.stringify(annotations),
        contentType: 'application/json'
      });
    },

    deleteAnnotation : function(id) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.deleteAnnotation(id).ajax();
    },

    deleteAnnotationBatch : function(ids) {
      return jsRoutes.controllers.api.annotation.AnnotationAPIController.bulkDelete().ajax({
        type: 'DELETE',
        data: JSON.stringify(ids),
        contentType: 'application/json'
      });
    },

    /** Place API **/

    getPlace : function(uri) {
      return jsRoutes.controllers.api.entity.PlaceAPIController.findPlaceByURI(uri).ajax();
    },

    listPlacesInDocument : function(docId, offset, size) {
      var o = (offset) ? offset : 0,
          s = (size) ? size : 20;

      return jsRoutes.controllers.api.entity.PlaceAPIController.listPlacesInDocument(docId, o, s).ajax();
    },

    searchPlaces : function(query, offset, size) {
      var o = (offset) ? offset : 0,
          s = (size) ? size : 20,
          searchAllGazetteers =
            (Config.authorities.gazetteers.hasOwnProperty('use_all')) ?
              Config.authorities.gazetteers.use_all : true,
          includes = (searchAllGazetteers) ? false :
            Config.authorities.gazetteers.includes;

      if (searchAllGazetteers)
        return jsRoutes.controllers.api.entity.PlaceAPIController
          .searchPlaces(query, o, s)
          .ajax();
      else
        return jsRoutes.controllers.api.entity.PlaceAPIController
          .searchPlaces(query, o, s, null, includes)
          .ajax()
          .then(function(results) {
            // Modify in place - not ideal...
            results.items = PlaceUtils.filterRecords(results.items, includes);
            return results;
          });
    }

  };

});
