require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/utils/annotationUtils',
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/baseApp',
  'document/annotation/text/page/toolbar',
  'document/annotation/text/selection/highlighter',
  'document/annotation/text/selection/selectionHandler',
], function(
  AnnotationUtils,
  API,
  Config,
  ReadEditor,
  WriteEditor,
  BaseApp,
  Toolbar,
  Highlighter,
  SelectionHandler) {

  var App = function() {

    var contentNode = document.getElementById('content'),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        highlighter = new Highlighter(contentNode),

        selector = new SelectionHandler(contentNode, highlighter),

        editor = (Config.writeAccess) ?
          new WriteEditor(contentNode, selector) :
          new ReadEditor(contentNode),

        colorschemeStylesheet = jQuery('#colorscheme'),

        initPagePrefs = function() {
          var storedColorscheme = localStorage.getItem('r2.document.edit.colorscheme'),
              colorscheme = (storedColorscheme) ? storedColorscheme : 'BY_STATUS';

          setColorscheme(colorscheme);
          toolbar.setCurrentColorscheme(colorscheme);

          if (Config.IS_TOUCH)
            contentNode.className = 'touch';
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

    BaseApp.apply(this, [ highlighter, selector ]);

    selector.on('select', editor.openSelection);

    editor.on('createAnnotation', this.onCreateAnnotation.bind(this));
    editor.on('updateAnnotation', this.onUpdateAnnotation.bind(this));
    editor.on('deleteAnnotation', this.onDeleteAnnotation.bind(this));

    rangy.init();

    initPagePrefs();

    API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
       .done(this.onAnnotationsLoaded.bind(this))
       .fail(this.onAnnotationsLoadError.bind(this));
  };
  App.prototype = Object.create(BaseApp.prototype);

  /** override - the tex UI needs annotations sorted by char offset, descending **/
  App.prototype.onAnnotationsLoaded = function(annotations) {
    var sorted = AnnotationUtils.sortByOffsetDesc(annotations);
    BaseApp.prototype.onAnnotationsLoaded.call(this, sorted);
  };

  /** Start on page load **/
  jQuery(document).ready(function() { new App(); });

});
