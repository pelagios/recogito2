require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/image/page/toolbar',
  'document/annotation/image/selection/pointHighlighter',
  'document/annotation/image/selection/pointSelectionHandler'],

  function(API, Config, WriteEditor, Toolbar, PointHighlighter, PointSelectionHandler) {

  jQuery(document).ready(function() {

    var contentNode = document.getElementById('image-pane'),

        toolbar = new Toolbar(),

        BASE_URL = jsRoutes.controllers.document.DocumentController
          .getImageTile(Config.documentId, Config.partSequenceNo, '').absoluteURL(),

        loadManifest = function() {
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
          // TODO handle difference between Zoomify and IIIF, based on Config.contentType
          var w = imageProperties.width,

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

              onAnnotationsLoaded = function(annotations) {
                highlighter.initPage(annotations);
              },

              onAnnotationsLoadError = function(annotations) {
                // TODO visual notification
              },

              onUpdateAnnotation = function(annotationStub) {
                API.storeAnnotation(annotationStub)
                   .done(function(annotation) {
                     // Merge server-provided properties (id, timestamps, etc.) into the annotation
                     jQuery.extend(annotationStub, annotation);
                     highlighter.refreshAnnotation(annotationStub);
                   })
                   .fail(function(error) {
                     // TODO show save error in UI
                     // TODO re-use 'header' class from text annotation UI
                     console.log(error);
                   });
              },

              onDeleteAnnotation = function(annotation) {
                API.deleteAnnotation(annotation.annotation_id)
                   .done(function() {
                     /*
                     header.incrementAnnotationCount(-1);
                     header.showStatusSaved();
                     */
                   })
                   .fail(function(error) {
                     // header.showSaveError(error);
                   });
              },

              onMapMove = function() {
                var selection = selector.getSelection();
                if (selection)
                  editor.setPosition(selection.bounds);
              };

          // TODO refactor, so we have one common 'annotation app' base class
          editor.on('updateAnnotation', onUpdateAnnotation);
          editor.on('deleteAnnotation', onDeleteAnnotation);

          olMap.on('postrender', onMapMove);

          API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
             .done(onAnnotationsLoaded)
             .fail(onAnnotationsLoadError);
        };

    loadManifest()
      .done(init)
      .fail(onLoadError);
  });

});
