require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: {
    marked: '/webjars/marked/0.3.6/marked.min',
    i18n: '../vendor/i18n'
  }
});

require([
  'common/ui/alert',
  'common/annotationView',
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/page/loadIndicator',
  'document/annotation/common/baseApp',
  'document/annotation/image/iiif/iiifImageInfo',
  'document/annotation/image/page/help',
  'document/annotation/image/page/toolbar',
  'document/annotation/image/page/viewer',
  'document/annotation/image/selection/highlighter',
  'document/annotation/image/selection/selectionHandler'
], function(
  Alert,
  AnnotationView,
  API,
  Config,
  ReadEditor,
  WriteEditor,
  LoadIndicator,
  BaseApp,
  IIIFImageInfo,
  Help,
  Toolbar,
  Viewer,
  Highlighter,
  SelectionHandler
) {

    var loadIndicator = new LoadIndicator();

    /** The app is instantiated after the image manifest was loaded **/
    var App = function(imageProperties) {

      var self = this,

          annotations = new AnnotationView(),

          contentNode = document.getElementById('image-pane'),

          toolbar = new Toolbar(),

          viewer = new Viewer(imageProperties),

          olMap = viewer.olMap,

          highlighter = new Highlighter(olMap),

          selector = new SelectionHandler(contentNode, olMap, highlighter),

          editor = (Config.writeAccess) ?
            new WriteEditor(contentNode, annotations.readOnly(), selector, { autoscroll: false }) :
            new ReadEditor(contentNode, annotations.readOnly(), { autoscroll: false }),

          help = new Help(),

          onToggleFullscreen = function(isFullscreen) {
            selector.updateSize();
          },

          onMapMove = function() {
            if (editor.isOpen()) {
              var selection = selector.getSelection();
              if (selection) editor.setPosition(selection.bounds);
            }
          },

          onToolChanged = function(toolKey) {
            if (toolKey === 'move') {
              jQuery(contentNode).removeClass('edit');
              editor.close();
              selector.setEnabled(false);
            } else {
              jQuery(contentNode).addClass('edit');
              selector.setEnabled(toolKey);
            }
          },

          onOverlayColorChanged = function(color) {
            highlighter.setOverlayColor(color);
          },

          onToggleHelp = function() {
            if (help.isVisible())
              help.close();
            else
              help.open();
          },

          /**
           * Helper that wraps a function with preceding selector.clearSelection.
           * We need this due to the difference between text and image annotation.
           * In image annotation annotation updates and deletes need an explicit
           * call to selector.clear. In text annotation, this isn't needed, so the
           * BaseApp doesn't do it.
           */
          withClearSelection = function(fn) {
            return function(arg) {
              selector.clearSelection();
              fn(arg);
            };
          };

      toolbar.on('toolChanged', onToolChanged);
      toolbar.on('toggleHelp', onToggleHelp);
      toolbar.on('overlayColorChanged', onOverlayColorChanged);
      toolbar.on('toggleOverlays', highlighter.toggleOverlays);

      BaseApp.apply(this, [ annotations, highlighter, selector ]);

      viewer.on('fullscreen', onToggleFullscreen);

      selector.on('changeShape', editor.updateAnchor.bind(editor));
      selector.on('select', editor.openSelection.bind(editor));

      editor.on('createAnnotation', this.onCreateAnnotation.bind(this));
      editor.on('updateAnnotation', withClearSelection(this.onUpdateAnnotation.bind(this)));
      editor.on('deleteAnnotation', withClearSelection(this.onDeleteAnnotation.bind(this)));

      olMap.on('postrender', onMapMove);

      API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
         .done(this.onAnnotationsLoaded.bind(this)).then(loadIndicator.destroy)
         .fail(this.onAnnotationsLoadError.bind(this)).then(loadIndicator.destroy);
    };
    App.prototype = Object.create(BaseApp.prototype);

    /** On page load, fetch the manifest and instantiate the app **/
    jQuery(document).ready(function() {

      var imagePane = document.getElementById('image-pane'),

          loadManifest = function() {
            return jsRoutes.controllers.document.DocumentController
              .getImageManifest(Config.documentId, Config.partSequenceNo)
              .ajax({ crossDomain: true })
              .then(function(response) {
                if (Config.contentType === 'IMAGE_UPLOAD') {
                  // jQuery handles the XML parsing
                  var props = jQuery(response).find('IMAGE_PROPERTIES'),
                      width = parseInt(props.attr('WIDTH')),
                      height = parseInt(props.attr('HEIGHT'));

                  return { width: width, height: height };
                } else if (Config.contentType === 'IMAGE_IIIF') {
                  return new IIIFImageInfo(response);
                }
              });
          },

          /**
           * We need the image pane to extend from below the header bar to the bottom of the
           * page. Since the header bar has a non-fixed height, this is non-trivial to do
           * in CSS. (I guess there's a way to do it using flexbox. But then the page structure
           * would likely have to be different for image and text views, which complicates
           * matters. Easier to determine the offset via JS at page load.)
           */
          setImagePaneTop = function() {
            var iconbar = jQuery('.header-iconbar'),
                infobox = jQuery('.header-infobox'),
                toolbar = jQuery('.header-toolbar'),

                offset = iconbar.outerHeight() + infobox.outerHeight() + toolbar.outerHeight();

            jQuery(imagePane).css('top', offset);
          },

          onLoadError = function(error) {
            var title = 'Connection Error',
                message = (Config.contentType === 'IMAGE_IIIF') ?
                  'There was an error connecting to the remote IIIF endpoint.' :
                  'An error occured.',
                alert = new Alert(Alert.ERROR, title, message);
          },

          init = function(imageProperties) {
            new App(imageProperties);
          };

      new Blazy({ // Init image lazy loading lib
        offset: 0,
        container: '.sidebar .menu',
        validateDelay: 200,
        saveViewportOffsetDelay: 200
      });

      setImagePaneTop();
      loadIndicator.init(imagePane);

      loadManifest()
        .done(init)
        .fail(onLoadError);
    });

});
