require.config({
  baseUrl: "/assets/javascripts/"
});

require([
  'document/annotation/text/editor/editor',
  'document/annotation/text/page/header',
  'document/annotation/text/page/toolbar',
  'document/annotation/text/selection/highlighter',
  'common/utils/annotationUtils',
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
          var highlighter = new Highlighter(contentNode),
              sorted = AnnotationUtils.sortByOffsetDesc(annotations);

          header.incrementAnnotationCount(annotations.length);
          // var startTime = new Date().getTime();
          highlighter.initPage(sorted);
          // console.log('took ' + (new Date().getTime() - startTime) + 'ms');
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
               AnnotationUtils.bindToElements(annotation, e.elements);
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
