define([
  'common/utils/annotationUtils',
  'document/annotation/common/selection/reapply/delete/modal'
], function(AnnotationUtils, Modal) {

  var ReDelete = function(annotations) {

    var actionHandlers = {},

        onDelete = function(toDelete) {
          actionHandlers.delete(toDelete);
        },

        onGoAdvanced = function() {
          console.log('advanced');
        },

        reapplyDelete = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),
              annotated = annotations.filterByQuote(quote).filter(function(a) {
                // Don't doublecount the current annotation
                return a.annotation_id != annotation.annotation_id;
              });

          if (annotated.length > 0)
            Modal.prompt(quote, annotated,
              onDelete.bind(this, annotated),
              onGoAdvanced.bind(this, annotated));
        },

        on = function(evt, handler) {
          actionHandlers[evt] = handler;
        };

    this.reapplyDelete = reapplyDelete;
    this.on = on;
  };

  return ReDelete;

});
