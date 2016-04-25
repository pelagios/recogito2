define(['../common/hasEvents'], function(HasEvents) {

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
        self.fireEvent('annotationsLoaded', data);
      },

      error: function(error) {
        self.fireEvent('loadError', error);
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
        self.fireEvent('createSuccess', annotation);
      },

      error: function(error) {
        self.fireEvent('createError', error);
      }
    });
  };

  return APIConnector;

});
