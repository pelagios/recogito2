require([
  'events',
  'highlighter',
  'selectionHandler',
  'toolbar',
  'editor/editor',
  '../apiConnector',
  '../../common/config',
  '../../common/annotationUtils'], function(Events, Highlighter, SelectionHandler, Toolbar, Editor, APIConnector, Config, Utils) {

  jQuery(document).ready(function() {

    var toolbar = new Toolbar(jQuery('.header-toolbar')),

        contentNode = document.getElementById('content'),

        textNode = contentNode.childNodes[0],

        highlighter = new Highlighter(contentNode),

        selectionHandler = new SelectionHandler(contentNode),

        // TODO just for testing & & formatting - clean up later
        editor = new Editor(),

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

        onAnnotationStored = function(annotation) {
          renderAnnotation(annotation);
        },

        onAnnotationStoreError = function(error) {
          // TODO visual notification
          console.log(error);
        },

        onAnnotationCreated = function(annotationStub) {
          // TODO just a dummy for now
          annotationStub.bodies.push({ type: toolbar.getCurrentMode().type });
          API.storeAnnotation(annotationStub);
        };

    // API event handling
    API.on(Events.API_ANNOTATIONS_LOADED, onAnnotationsLoaded);
    API.on(Events.API_ANNOTATIONS_LOAD_ERROR, onAnnotationsLoadError);
    API.on(Events.API_CREATE_SUCCESS, onAnnotationStored);
    API.on(Events.API_CREATE_ERROR, onAnnotationStoreError);

    // Annotation event handling
    selectionHandler.on(Events.ANNOTATION_CREATED, onAnnotationCreated);

    // Init
    rangy.init();
    API.loadAnnotations(Config.documentId, Config.partSequenceNo);
  });

});
