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

        textNode = contentNode.childNodes[0],

        highlighter = new Highlighter(contentNode),

        editor = new Editor(contentNode),

        API = new APIConnector(),

        renderAnnotation = function(annotation) {
          var anchor = annotation.anchor.substr(12),
              quote = Utils.getQuote(annotation),
              entityType = Utils.getEntityType(annotation),
              range = rangy.createRange();

          range.selectCharacters(textNode, parseInt(anchor), parseInt(anchor) + quote.length);
          highlighter.wrapRange(range, 'entity ' + entityType);
        },

        onAnnotationsLoaded = function(annotations) {
          var sorted = Utils.sortByOffset(annotations);

          jQuery('.annotations').html(annotations.length);

          // TODO revise, so that DOM manipulation happens in batches, with a
          // bit of wait time in between, so that we don't freeze the browser
          jQuery.each(sorted, function(idx, annotation) {
            renderAnnotation(annotation);
          });
        },

        onAnnotationsLoadError = function(annotations) {
          // TODO visual notification
        },

        onAnnotationUpdated = function(e) {

        },

        onAnnotationStored = function(annotation) {
          // renderAnnotation(annotation);
        },

        onAnnotationStoreError = function(error) {
          // TODO visual notification
          console.log(error);
        };

    // Editor events
    editor.on('ok', onAnnotationUpdated);

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
