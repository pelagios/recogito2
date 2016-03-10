require([
  '../apiConnector',
  'events',
  'highlighter',
  'selectionHandler',
  '../../common/config',
  '../../common/annotationUtils'], function(APIConnector, Events, Highlighter, SelectionHandler, Config, Utils) {

  jQuery(document).ready(function() {

    var toolbar = jQuery('.header-toolbar'),

        contentNode = document.getElementById('content'),

        textNode = contentNode.childNodes[0],

        highlighter = new Highlighter(contentNode),

        selectionHandler = new SelectionHandler(contentNode),

        API = new APIConnector(),

        makeToolbarSticky = function() {
          var onScroll = function() {
            var scrollTop = jQuery(window).scrollTop();
            if (scrollTop > 167)
              toolbar.addClass('fixed');
            else
              toolbar.removeClass('fixed');
          };

          jQuery(window).scroll(onScroll);
          if (Config.IS_TOUCH)
            jQuery(window).addEventListener('touchmove', onScroll, false);
        },

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

        onAnnotationCreated = function(annotationStub) {
          API.storeAnnotation(annotationStub);
        },

        onAnnotationStored = function(annotation) {
          renderAnnotation(annotation);
        },

        onAnnotationStoreError = function(error) {
          // TODO visual notification
          console.log(error);
        };

    makeToolbarSticky();

    API.on(Events.API_ANNOTATIONS_LOADED, onAnnotationsLoaded);
    API.on(Events.API_ANNOTATIONS_LOAD_ERROR, onAnnotationsLoadError);
    API.on(Events.API_CREATE_SUCCESS, onAnnotationStored);
    API.on(Events.API_CREATE_ERROR, onAnnotationStoreError);

    selectionHandler.on(Events.ANNOTATION_CREATED, onAnnotationCreated);

    rangy.init();

    API.loadAnnotations(Config.documentId, Config.partSequenceNo);
  });

});
