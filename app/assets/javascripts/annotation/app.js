require(['annotationRenderer', 'highlighter', 'selectionHandler'], function(AnnotationRenderer, Highlighter, SelectionHandler) {

  jQuery(document).ready(function() {
    var highlighter = new Highlighter();

    rangy.init();

    // Load annotations
    jsRoutes.controllers.annotation.TextAnnotationController.getAnnotationsFor(window.config.filepartId).ajax({
      success: function(data) {
        var rootNode = document.getElementById('content'),
            renderer = new AnnotationRenderer(rootNode, data, highlighter),
            selectionHandler = new SelectionHandler(rootNode, highlighter);

        jQuery('.annotations').html(data.length);
      },

      error: function(error) {
        console.log(error);
      }
    });
  });

});
