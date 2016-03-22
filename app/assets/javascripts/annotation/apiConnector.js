define(['../common/hasEvents', './apiEvents'], function(HasEvents, Events) {

  var APIConnector = function() {
    HasEvents.apply(this);
  };
  APIConnector.prototype = Object.create(HasEvents.prototype);

  APIConnector.prototype.loadAnnotations = function(documentId, partNumber) {
    var self = this;
    jsRoutes.controllers.api.AnnotationAPIController.loadAnnotations().ajax({
      data: {
        doc: documentId,
        part: partNumber
      },

      success: function(data) {
        self.fireEvent(Events.API_ANNOTATIONS_LOADED, data);
      },

      error: function(error) {
        self.fireEvent(Events.API_ANNOTATIONS_LOAD_ERROR, error);
      }
    });
  };

  APIConnector.prototype.storeAnnotation = function(annotationStub) {
    var self = this;
    jsRoutes.controllers.api.AnnotationAPIController.createAnnotation().ajax({
      type: 'POST',
      data: JSON.stringify(annotationStub),
      contentType: 'application/json',

      success: function(annotation) {
        self.fireEvent(Events.API_CREATE_SUCCESS, annotation);
      },

      error: function(error) {
        self.fireEvent(Events.API_CREATE_ERROR, error);
      }
    });
  };

  return APIConnector;

});
