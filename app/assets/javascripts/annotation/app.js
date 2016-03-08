require(['annotationRenderer', 'highlighter', 'selectionHandler'], function(AnnotationRenderer, Highlighter, SelectionHandler) {

  jQuery(document).ready(function() {
    var highlighter = new Highlighter(),

        toolbar = jQuery('.header-toolbar'),

        makeToolbarSticky = function() {
          jQuery(window).scroll(function() {
            var scrollTop = jQuery(window).scrollTop();
            if (scrollTop > 167)
              toolbar.addClass('fixed');
            else
              toolbar.removeClass('fixed');
          });
        };

    makeToolbarSticky();
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
