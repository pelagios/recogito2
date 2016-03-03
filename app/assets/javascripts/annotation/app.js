require(['annotationRenderer', 'selectionHandler'], function(AnnotationRenderer, SelectionHandler) {

  jQuery(document).ready(function() {
    // Load annotations
    jsRoutes.controllers.annotation.TextAnnotationController.getAnnotationsFor(window.config.filepartId).ajax({
      success: function(data) {
        var rootNode = document.getElementById('content'),
            renderer = new AnnotationRenderer(rootNode, data),
            selectionHandler = new SelectionHandler(rootNode);
      },

      error: function(error) {
        console.log(error);
      }
    });
  });

});
