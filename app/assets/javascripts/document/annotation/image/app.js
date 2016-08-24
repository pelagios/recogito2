require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/baseApp',
  'document/annotation/image/page/toolbar',
  'document/annotation/image/selection/pointHighlighter',
  'document/annotation/image/selection/pointSelectionHandler'],

  function(API, Config, WriteEditor, BaseApp, Toolbar, PointHighlighter, PointSelectionHandler) {

    /** The app is instantiated after the image manifest was loaded **/
    var App = function(imageProperties) {

      // TODO handle difference between Zoomify and IIIF, based on Config.contentType
      var contentNode = document.getElementById('image-pane'),

          toolbar = new Toolbar(),

          BASE_URL = jsRoutes.controllers.document.DocumentController
            .getImageTile(Config.documentId, Config.partSequenceNo, '').absoluteURL(),

          w = imageProperties.width,

          h = imageProperties.height,

          projection = new ol.proj.Projection({
            code: 'ZOOMIFY',
            units: 'pixels',
            extent: [0, 0, w, h]
          }),

          tileSource = new ol.source.Zoomify({
            url: BASE_URL,
            size: [ w, h ]
          }),

          tileLayer = new ol.layer.Tile({ source: tileSource }),

          olMap = new ol.Map({
            target: 'image-pane',
            layers: [ tileLayer ],
            view: new ol.View({
              projection: projection,
              center: [w / 2, - (h / 2)],
              zoom: 0,
              minResolution: 0.125
            })
          }),

          highlighter = new PointHighlighter(olMap),

          selector = new PointSelectionHandler(contentNode, olMap, highlighter),

          editor = new WriteEditor(contentNode, highlighter, selector),

          onMapMove = function() {
            var selection = selector.getSelection();
            if (selection)
              editor.setPosition(selection.bounds);
          },

          /** TODO so far, dummy implementation **/
          onToolChanged = function(toolName) {
            if (toolName === 'POINT')
              selector.setEnabled(true);
            else
              selector.setEnabled(false);
          };

      toolbar.on('toolChanged', onToolChanged);

      BaseApp.apply(this, [ editor, highlighter ]);

      // TODO refactor, so we have one common 'annotation app' base class
      editor.on('updateAnnotation', this.onUpdateAnnotation.bind(this));
      editor.on('deleteAnnotation', this.onDeleteAnnotation.bind(this));

      olMap.on('postrender', onMapMove);

      API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
         .done(this.onAnnotationsLoaded.bind(this))
         .fail(this.onAnnotationsLoadError.bind(this));
    };
    App.prototype = Object.create(BaseApp.prototype);

    /** On page load, fetch the manifest and instantiate the app **/
    jQuery(document).ready(function() {

      var loadManifest = function() {
            return jsRoutes.controllers.document.DocumentController
              .getImageManifest(Config.documentId, Config.partSequenceNo)
              .ajax()
              .then(function(response) {
                // TODO handle difference between Zoomify and IIIF, based on Config.contentType

                // jQuery handles the XML parsing
                var props = jQuery(response).find('IMAGE_PROPERTIES'),
                    width = parseInt(props.attr('WIDTH')),
                    height = parseInt(props.attr('HEIGHT')),
                    imageProperties = { width: width, height: height };

                // Store image properties in global Config as well
                Config.imageProperties = imageProperties;
                return imageProperties;
              });
          },

          onLoadError = function(error) {
            // TODO error indication to user
            console.log(error);
          },

          init = function(imageProperties) {
            new App(imageProperties);
          };

      loadManifest()
        .done(init)
        .fail(onLoadError);
    });

});
