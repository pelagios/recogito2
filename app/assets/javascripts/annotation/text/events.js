define(['../apiEvents'], function(APIEvents) {

  var TextAnnotationEvents = {

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* UI control events              */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    ANNOTATION_MODE_CHANGED : 'annotationModeChanged',

    COLOR_SCHEME_CHANGED: 'colorSchemeChanged',

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    /* Annotation events              */
    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    ANNOTATION_CREATED : 'annotationCreated'

  };

  /** Combination of common API with view-specific text annotation events **/
  return jQuery.extend({}, APIEvents, TextAnnotationEvents);

});
