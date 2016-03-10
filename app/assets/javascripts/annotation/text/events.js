define(['../apiEvents'], function(APIEvents) {

  var TextAnnotationEvents = {

    ANNOTATION_CREATED : 'annotationCreated'

  };

  /** Combination of common API with view-specific text annotation events **/
  return jQuery.extend({}, APIEvents, TextAnnotationEvents);

});
