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
  'document/annotation/text/selection/phraseAnnotator'
], function(BaseTextApp, Highlighter, SelectionHandler, PhraseAnnotator) {

  jQuery(document).ready(function() {
    var contentNode = document.getElementById('content'),
        highlighter = new Highlighter(contentNode),
        selector = new SelectionHandler(contentNode, highlighter),
        phraseAnnotator = new PhraseAnnotator(contentNode, highlighter);

    new BaseTextApp(contentNode, highlighter, selector, phraseAnnotator);

    // TODO for testing!
    var options = {
      root: null, // viewport
      threshold: 1.0
    };

    var callback = function(entries) {
      if (entries[0].isIntersecting) {
        console.log('visible!');
        console.log(entries);
      }
    };
    
    var targets = document.querySelectorAll('.visibility-marker');
    for(var i=0; i<targets.length; i++) {
      var observer = new IntersectionObserver(callback, options);
      observer.observe(targets[i]);
    }
  });

});
