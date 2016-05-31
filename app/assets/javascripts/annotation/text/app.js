require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'annotation/text/editor/editor',
  'annotation/text/page/header',
  'annotation/text/page/toolbar',
  'annotation/text/selection/highlighter',
  'common/helpers/annotationUtils',
  'common/api',
  'common/config'],

  function(Editor, Header, Toolbar, Highlighter, AnnotationUtils, API, Config) {

  jQuery(document).ready(function() {

    var header = new Header(),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        contentNode = document.getElementById('content'),

        editor = new Editor(contentNode),

        colorschemeStylesheet = jQuery('#colorscheme'),

        onAnnotationsLoaded = function(annotations) {
              // Number of annotations to render in one go
          var BATCH_SIZE = 50,

              // Idle time between batchs, in milliseconds
              IDLETIME_MS = 500,

              highlighter = new Highlighter(contentNode),

              sorted = AnnotationUtils.sortByOffset(annotations),

              renderInBatches = function(annotations) {
                var batch = annotations.slice(0, BATCH_SIZE),
                    remainder = annotations.slice(BATCH_SIZE);

                jQuery.each(batch, function(idx, annotation) {
                  highlighter.renderAnnotation(annotation);
                });

                if (remainder.length > 0)
                  setTimeout(function() {
                    renderInBatches(remainder);
                  }, IDLETIME_MS);
              };

          header.incrementAnnotationCount(annotations.length);
          renderInBatches(sorted);
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
        },

        onColorModeChanged = function(mode) {
          var currentCSSPath = colorschemeStylesheet.attr('href'),
              basePath = currentCSSPath.substr(0, currentCSSPath.lastIndexOf('/'));

          if (mode === 'BY_TYPE')
            colorschemeStylesheet.attr('href', basePath + '/colorByType.css');
          else if (mode === 'BY_STATUS')
            colorschemeStylesheet.attr('href', basePath + '/colorByStatus.css');
        };

    // Toolbar events
    toolbar.on('annotationModeChanged', editor.setAnnotationMode);
    toolbar.on('colorModeChange', onColorModeChanged);

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
