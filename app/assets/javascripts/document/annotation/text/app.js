require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/text/page/header',
  'document/annotation/text/page/toolbar',
  'document/annotation/text/selection/highlighter',
  'document/annotation/text/selection/selectionHandler',
  'common/utils/annotationUtils',
  'common/api',
  'common/config'],

  function(ReadEditor, WriteEditor, Header, Toolbar, Highlighter, SelectionHandler,
    AnnotationUtils, API, Config) {

  jQuery(document).ready(function() {

    var header = new Header(),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        contentNode = document.getElementById('content'),

        highlighter = new Highlighter(contentNode),

        editor = (Config.writeAccess) ?
          new WriteEditor(contentNode, highlighter, new SelectionHandler(contentNode, highlighter)) :
          new ReadEditor(contentNode, highlighter),

        colorschemeStylesheet = jQuery('#colorscheme'),

        initPagePrefs = function() {
          var storedColorscheme = localStorage.getItem('r2.document.edit.colorscheme'),
              colorscheme = (storedColorscheme) ? storedColorscheme : 'BY_STATUS';

          setColorscheme(colorscheme);
          toolbar.setCurrentColorscheme(colorscheme);

          if (Config.IS_TOUCH)
            contentNode.className = 'touch';
        },

        onAnnotationsLoaded = function(annotations) {
          var sorted = AnnotationUtils.sortByOffsetDesc(annotations),
              hash = (window.location.hash) ? window.location.hash.substring(1) : false,
              preselected;

          header.incrementAnnotationCount(annotations.length);
          // var startTime = new Date().getTime();
          highlighter.initPage(sorted);
          // console.log('took ' + (new Date().getTime() - startTime) + 'ms');

          if (hash) {
            // An annotation was pre-selected via URL hash
            preselected = highlighter.findById(hash);
            if (preselected)
              editor.open(preselected.annotation, preselected.elements[0].getBoundingClientRect());
          }
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

               // Merge server-provided properties (id, timestamps, etc.) into the annotation
               jQuery.extend(e.annotation, annotation);
               highlighter.refreshAnnotation(e.annotation);
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

        setColorscheme = function(mode) {
          var currentCSSPath = colorschemeStylesheet.attr('href'),
              basePath = currentCSSPath.substr(0, currentCSSPath.lastIndexOf('/'));

          if (mode === 'BY_TYPE')
            colorschemeStylesheet.attr('href', basePath + '/colorByType.css');
          else
            colorschemeStylesheet.attr('href', basePath + '/colorByStatus.css');
        },

        onColorschemeChanged = function(mode) {
          setColorscheme(mode);
          localStorage.setItem('r2.document.edit.colorscheme', mode);
        };

    // Toolbar events
    toolbar.on('annotationModeChanged', editor.setAnnotationMode);
    toolbar.on('colorschemeChanged', onColorschemeChanged);

    // Editor events
    editor.on('updateAnnotation', onUpdateAnnotation);
    editor.on('deleteAnnotation', onDeleteAnnotation);

    // Init
    rangy.init();

    // Init page preferences - e.g. color mode
    initPagePrefs();

    API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
       .done(onAnnotationsLoaded)
       .fail(onAnnotationsLoadError);
  });

});
