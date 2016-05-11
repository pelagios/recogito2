require([
  '../../common/helpers/annotationUtils',
  '../../common/api',
  '../../common/config',
  'editor/editor',
  'header',
  'highlighter',
  'toolbar'], function(AnnotationUtils, API, Config, Editor, Header, Highlighter, Toolbar) {

  jQuery(document).ready(function() {

    var header = new Header(),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        contentNode = document.getElementById('content'),

        editor = new Editor(contentNode),

        onAnnotationsLoaded = function(annotations) {
          var highlighter = new Highlighter(contentNode),
              sorted = AnnotationUtils.sortByOffset(annotations);

          header.incrementAnnotationCount(annotations.length);

          // TODO revise, so that DOM manipulation happens in batches, with a
          // bit of wait time in between, so that we don't freeze the browser
          jQuery.each(sorted, function(idx, annotation) {
            highlighter.renderAnnotation(annotation);
          });
        },

        onAnnotationsLoadError = function(annotations) {
          // TODO visual notification
        },

        onUpdateAnnotation = function(e) {
          header.showStatusSaving();

          API.storeAnnotation(e.annotation)
             .done(function(annotation) {
               // Update header info
               header.incrementAnnotationCount();
               header.updateContributorInfo(Config.me);
               header.showStatusSaved();

               // Update the annotation references in the elements
               jQuery.each(e.elements, function(idx, el) {
                 AnnotationUtils.attachAnnotation(el, annotation);
               });
             })
             .fail(function(error) {
               header.showSaveError(error);
             });
        },

        onDeleteAnnotation = function(annotation) {
          header.showStatusSaving();

          API.deleteAnnotation(annotation.annotation_id)
             .done(function() {
               header.incrementAnnotationCount(-1);
               header.showStatusSaved();
             })
             .fail(function(error) {
               header.showSaveError(error);
             });
        };

    // Toolbar events
    toolbar.on('annotationModeChanged', editor.setMode);

    // Editor events
    editor.on('updateAnnotation', onUpdateAnnotation);
    editor.on('deleteAnnotation', onDeleteAnnotation);

    // Init
    rangy.init();

    API.getAnnotationsForPart(Config.documentId, Config.partSequenceNo)
       .done(onAnnotationsLoaded)
       .fail(onAnnotationsLoadError);
  });

});
