define([
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/annotationView',
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/page/loadIndicator',
  'document/annotation/common/selection/reapply/reapply',
  'document/annotation/common/baseApp',
  'document/annotation/text/page/toolbar',
  'document/annotation/text/relations/relationsLayer'
], function(
  Formatting,
  AnnotationUtils,
  PlaceUtils,
  AnnotationView,
  API,
  Config,
  ReadEditor,
  WriteEditor,
  LoadIndicator,
  Reapply,
  BaseApp,
  Toolbar,
  RelationsLayer) {

  var App = function(contentNode, highlighter, selector, phraseAnnotator) {

    var self = this,

        annotationView = new AnnotationView(),

        annotations = annotationView.readOnly(),

        loadIndicator = new LoadIndicator(),

        containerNode = document.getElementById('main'),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        editor = (Config.writeAccess) ?
          new WriteEditor(containerNode, annotations, selector) :
          new ReadEditor(containerNode, annotations),

        reapply = new Reapply(phraseAnnotator, annotations),

        colorschemeStylesheet = jQuery('#colorscheme'),

        relationsLayer = new RelationsLayer(containerNode, document.getElementById('relations')),

        initPage = function() {
          var storedColorscheme = localStorage.getItem('r2.document.edit.colorscheme'),
              colorscheme = (storedColorscheme) ? storedColorscheme : 'BY_STATUS';

          setColorscheme(colorscheme);
          toolbar.setCurrentColorscheme(colorscheme);

          loadIndicator.init(containerNode);

          if (Config.IS_TOUCH)
            contentNode.className = 'touch';

          Formatting.initTextDirection(contentNode);
        },

        setColorscheme = function(mode) {
          var currentCSSPath = colorschemeStylesheet.attr('href'),
              basePath = currentCSSPath.substr(0, currentCSSPath.lastIndexOf('/'));

          highlighter.setColorscheme(false);

          if (mode === 'BY_TYPE') {
            colorschemeStylesheet.attr('href', basePath + '/colorByType.css');
          } else if (mode === 'BY_STATUS') {
            colorschemeStylesheet.attr('href', basePath + '/colorByStatus.css');
          } else {
            colorschemeStylesheet.attr('href', basePath + '/colorByProperty.css');
            highlighter.setColorscheme(mode); // All others are property-based schemes!
          }
        },

        onColorschemeChanged = function(mode) {
          setColorscheme(mode);
          localStorage.setItem('r2.document.edit.colorscheme', mode);
        },

        onCreateAnnotation = function(selection) {
          // Store the annotation first
          self.onCreateAnnotation(selection);

          // Then prompt the user if they want to re-apply across the doc
          reapply.reapplyIfNeeded(selection.annotation);
        },

        onUpdateAnnotation = function(annotationStub) {
          self.onUpdateAnnotation(annotationStub);
          reapply.reapplyIfNeeded(annotationStub);
        },

        onDeleteAnnotation = function(annotation) {
          relationsLayer.deleteRelationsTo(annotation.annotation_id);
          self.onDeleteAnnotation(annotation);
          reapply.reapplyDelete(annotation);
        },

        onUpdateRelations = function(annotation) {
          self.upsertAnnotation(annotation);
        },

        onAnnotationModeChanged = function(m) {
          if (m.mode === 'RELATIONS') {
            editor.close();
            selector.setEnabled(false);
            relationsLayer.setDrawingEnabled(true);
          } else {
            selector.setEnabled(true);
            relationsLayer.setDrawingEnabled(false);
            editor.setAnnotationMode(m);
          }
        },

        onTimefilterChanged = function(newerThan) {
          var content = jQuery('#content');

          // Clear filter
          annotations.forEach(function(annotation) {
            self.highlighter.removeClass(annotation, 'in-filter');
          });

          annotations.filter(function(annotation) {
            var lastModified = new Date(annotation.last_modified_at);
            return lastModified >= newerThan;
          }).forEach(function(annotation) {
            self.highlighter.addClass(annotation, 'in-filter');
          });
          
          // TODO clearing filter completely?
          content.addClass('filtered');
        };

    // Toolbar events
    toolbar.on('annotationModeChanged', onAnnotationModeChanged);
    toolbar.on('colorschemeChanged', onColorschemeChanged);
    toolbar.on('timefilterChanged', onTimefilterChanged);

    BaseApp.apply(this, [ annotationView, highlighter, selector ]);

    selector.on('select', editor.openSelection);

    reapply.on('create', self.onCreateAnnotationBatch.bind(self));
    reapply.on('update', self.upsertAnnotationBatch.bind(self));
    reapply.on('delete', self.onDeleteAnnotationBatch.bind(self));

    relationsLayer.on('updateRelations', onUpdateRelations);

    editor.on('createAnnotation', onCreateAnnotation);
    editor.on('updateAnnotation', onUpdateAnnotation);
    editor.on('deleteAnnotation', onDeleteAnnotation);

    rangy.init();

    initPage();

    PlaceUtils.initGazetteers().done(function() {
      API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
         .done(function(annotations) {
           toolbar.initTimefilter(annotations);
           self.onAnnotationsLoaded(annotations);
         })
         .then(relationsLayer.init)
         .then(loadIndicator.destroy)
         .fail(self.onAnnotationsLoadError.bind(self)).then(loadIndicator.destroy);
    });
  };
  App.prototype = Object.create(BaseApp.prototype);

  /** override - the tex UI needs annotations sorted by char offset, descending **/
  App.prototype.onAnnotationsLoaded = function(annotations) {
    var sorted = AnnotationUtils.sortByOffsetDesc(annotations);
    BaseApp.prototype.onAnnotationsLoaded.call(this, sorted);
  };

  return App;

});
