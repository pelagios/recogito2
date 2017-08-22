require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: { marked: '/webjars/marked/0.3.6/marked.min' }
});

require([
  'document/annotation/common/baseTextApp',
  'document/annotation/text/selection/highlighter',
  'document/annotation/text/selection/selectionHandler'
], function(BaseTextApp, Highlighter, SelectionHandler) {

  jQuery(document).ready(function() {
    var contentNode = document.getElementById('content'),
        highlighter = new Highlighter(contentNode),
        selector = new SelectionHandler(contentNode, highlighter);

    new BaseTextApp(contentNode, highlighter, selector);
  });

});
