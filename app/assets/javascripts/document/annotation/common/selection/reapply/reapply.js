define([
  'document/annotation/common/selection/reapply/annotate/reannotate',
  'document/annotation/common/selection/reapply/delete/redelete',
], function(ReAnnotate, ReDelete) {

  /**
   * Just delegates re-apply actions to child components that handle the
   * specific cases.
   */
  var Reapply = function(phraseAnnotator, annotations) {

    var reannotator = new ReAnnotate(phraseAnnotator, annotations),

        redeleter = new ReDelete(annotations),

        reapplyIfNeeded = function(annotation) {
          reannotator.reapplyIfNeeded(annotation);
        },

        reapplyDelete = function(annotation) {
          redeleter.reapplyDelete(annotation);
        },

        on = function(evt, handler) {
          reannotator.on(evt, handler);
          redeleter.on(evt, handler);
        };

    this.reapplyIfNeeded = reapplyIfNeeded;
    this.reapplyDelete = reapplyDelete;
    this.on = on;
  };

  return Reapply;

});
