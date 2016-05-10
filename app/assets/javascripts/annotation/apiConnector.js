define([], function() {

  var APIConnector = function() { };

  APIConnector.prototype.loadAnnotations = function(documentId, partNumber) {
    return jsRoutes.controllers.api.AnnotationAPIController.loadAnnotations().ajax({
      data: {
        doc: documentId,
        part: partNumber
      }
    });
  };

  APIConnector.prototype.storeAnnotation = function(annotation) {
    return jsRoutes.controllers.api.AnnotationAPIController.createAnnotation().ajax({
      type: 'POST',
      data: JSON.stringify(annotation),
      contentType: 'application/json'
    });
  };

  APIConnector.prototype.deleteAnnotation = function(id) {
    return jsRoutes.controllers.api.AnnotationAPIController.deleteAnnotation(id).ajax();
  };

  return APIConnector;

});
