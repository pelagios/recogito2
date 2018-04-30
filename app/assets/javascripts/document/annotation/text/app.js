require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/,
  paths: {
    marked: '/webjars/marked/0.3.6/marked.min',
    i18n: '../vendor/i18n'
  }
});

require([
  'document/annotation/common/baseTextApp',
  'document/annotation/text/selection/highlighter',
  'document/annotation/text/selection/selectionHandler',
  'document/annotation/text/selection/phraseAnnotator',
  'document/annotation/text/svg/scratchpad'
], function(BaseTextApp, Highlighter, SelectionHandler, PhraseAnnotator, ScratchPad) {

  jQuery(document).ready(function() {
    var contentNode = document.getElementById('content'),
        highlighter = new Highlighter(contentNode),
        selector = new SelectionHandler(contentNode, highlighter),
        phraseAnnotator = new PhraseAnnotator(contentNode, highlighter);

    // TODO dummy only
    var scratchPad = new ScratchPad();

    new BaseTextApp(contentNode, highlighter, selector, phraseAnnotator);

    selector.on('select', scratchPad.draw);
  });

});
