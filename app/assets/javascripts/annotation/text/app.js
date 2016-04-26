require([
  'highlighter',
  'toolbar',
  'editor/editor',
  '../apiConnector',
  '../../common/config',
  '../../common/annotationUtils'], function(Highlighter, Toolbar, Editor, APIConnector, Config, Utils) {

  jQuery(document).ready(function() {

    var toolbar = new Toolbar(jQuery('.header-toolbar')),

        contentNode = document.getElementById('content'),

        highlighter = new Highlighter(contentNode),

        editor = new Editor(contentNode),

        API = new APIConnector(),

        onAnnotationsLoaded = function(annotations) {
          var sorted = Utils.sortByOffset(annotations);

          jQuery('.annotations').html(annotations.length);

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
          API.storeAnnotation(e.annotation)
             .done(function(annotation) {
               // Update the annotation references in the elements
               jQuery.each(e.elements, function(idx, el) {
                 Utils.attachAnnotation(el, annotation);
               });
             })
             .fail(function(error) {
               console.log(error);
             });
        },

        onAnnotationStored = function(annotation) {
          // renderAnnotation(annotation);
        },

        onAnnotationStoreError = function(error) {
          // TODO visual notification
          console.log(error);
        };

    // Editor events
    editor.on('updateAnnotation', onUpdateAnnotation);

    // API events
    API.on('annotationsLoaded', onAnnotationsLoaded);
    API.on('loadError', onAnnotationsLoadError);
    API.on('createSuccess', onAnnotationStored);
    API.on('createError', onAnnotationStoreError);

    // Init
    rangy.init();

    API.loadAnnotations(Config.documentId, Config.partSequenceNo);
  });

});
