define([
  'common/utils/annotationUtils'
], function(AnnotationUtils) {

  var ReDelete = function(annotations) {

    var actionHandlers = {},

        reapplyDelete = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),
              annotated = annotations.filterByQuote(quote).filter(function(a) {
                // Don't doublecount the current annotation
                return a.annotation_id != annotation.annotation_id;
              });

          console.log('reapplying delete');
          console.log(annotation);
          console.log(annotated.length + ' to delete');
        },

        on = function(evt, handler) {
          actionHandlers[evt] = handler;
        };

    this.reapplyDelete = reapplyDelete;
    this.on = on;
  };

  return ReDelete;

});
