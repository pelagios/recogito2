require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/baseApp',
  'document/annotation/image/page/toolbar',
  'document/annotation/image/selection/highlighter',
  'document/annotation/image/selection/selectionHandler'],

  function(API, Config, ReadEditor, WriteEditor, BaseApp, Toolbar, Highlighter, SelectionHandler) {

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

          highlighter = new Highlighter(olMap),

          selector = new SelectionHandler(contentNode, olMap, highlighter),

          editor = (Config.writeAccess) ?
            new WriteEditor(contentNode, selector) :
            new ReadEditor(contentNode),

          onMapMove = function() {
            var selection = selector.getSelection();
            if (selection)
              editor.setPosition(selection.bounds);
          },

          onToolChanged = function(toolName) {
            if (toolName === 'MOVE') {
              jQuery(contentNode).removeClass('edit');
              selector.setEnabled(false);
            } else {
              jQuery(contentNode).addClass('edit');
              selector.setEnabled(toolName);
            }
          };

      toolbar.on('toolChanged', onToolChanged);

      BaseApp.apply(this, [ editor, highlighter ]);

      selector.on('select', editor.openSelection);

      editor.on('createAnnotation', this.onCreateAnnotation.bind(this));
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

          /**
           * We need the image pane to extend from below the header bar to the bottom of the
           * page. Since the header bar has a non-fixed height, this is non-trivial to do
           * in CSS. (I guess there's a way to do it using flexbox. But then the page structure
           * would likely have to be different for image and text views, which complicates
           * matters. Easier to determine the offset via JS at page load.)
           */
          setImagePaneTop = function() {
            var imagePane = jQuery('#image-pane'),

                iconbar = jQuery('.header-iconbar'),
                infobox = jQuery('.header-infobox'),
                toolbar = jQuery('.header-toolbar'),

                offset = iconbar.outerHeight() + infobox.outerHeight() + toolbar.outerHeight();

            imagePane.css('top', offset);
          },

          onLoadError = function(error) {
            // TODO error indication to user
            console.log(error);
          },

          init = function(imageProperties) {
            new App(imageProperties);
          };

      setImagePaneTop();

      loadManifest()
        .done(init)
        .fail(onLoadError);
    });

});
