require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/alert',
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/baseApp',
  'document/annotation/text/page/toolbar',
  'document/annotation/text/selection/highlighter',
  'document/annotation/text/selection/phraseAnnotator',
  'document/annotation/text/selection/selectionHandler',
], function(
  Alert,
  Formatting,
  AnnotationUtils,
  API,
  Config,
  ReadEditor,
  WriteEditor,
  BaseApp,
  Toolbar,
  Highlighter,
  PhraseAnnotator,
  SelectionHandler) {

  var App = function() {

    var self = this,

        contentNode = document.getElementById('content'),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        highlighter = new Highlighter(contentNode),

        phraseAnnotator = new PhraseAnnotator(contentNode, highlighter),

        selector = new SelectionHandler(contentNode, highlighter),

        editor = (Config.writeAccess) ?
          new WriteEditor(contentNode, selector) :
          new ReadEditor(contentNode),

        colorschemeStylesheet = jQuery('#colorscheme'),

        initPage = function() {
          var storedColorscheme = localStorage.getItem('r2.document.edit.colorscheme'),
              colorscheme = (storedColorscheme) ? storedColorscheme : 'BY_STATUS';

          setColorscheme(colorscheme);
          toolbar.setCurrentColorscheme(colorscheme);

          if (Config.IS_TOUCH)
            contentNode.className = 'touch';

          Formatting.initTextDirection(contentNode);
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
        },

        onCreateAnnotation = function(selection) {
          var reapply = function() {
                var selections = phraseAnnotator.createSelections(selection.annotation);
                jQuery.each(selections, function(idx, selection) {
                  // TODO roll into one request!
                  self.onCreateAnnotation(selection);
                });
              },

              promptReapply = function() {
                var quote = AnnotationUtils.getQuote(selection.annotation),
                    occurrenceCount = phraseAnnotator.countOccurrences(quote),
                    promptTitle = 'Re-Apply',
                    promptMessage = (occurrenceCount > 1) ?
                      'There are ' + occurrenceCount + ' more occurrences of <em>' :
                      'There is 1 more occurrence of <em>';

                if (occurrenceCount > 0) {
                  promptMessage +=
                    quote + '</em> in the text.<br/>Do you want to re-apply this annotation?';

                  new Alert(Alert.INFO, promptTitle, promptMessage).on('ok', reapply);
                }
              };

          // Store the annotation first
          self.onCreateAnnotation(selection);

          // Then prompt the user if they want to re-apply across the doc
          promptReapply();
        };

    // Toolbar events
    toolbar.on('annotationModeChanged', editor.setAnnotationMode);
    toolbar.on('colorschemeChanged', onColorschemeChanged);

    BaseApp.apply(this, [ highlighter, selector ]);

    selector.on('select', editor.openSelection);

    editor.on('createAnnotation', onCreateAnnotation);
    editor.on('updateAnnotation', this.onUpdateAnnotation.bind(this));
    editor.on('deleteAnnotation', this.onDeleteAnnotation.bind(this));

    rangy.init();

    initPage();

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
