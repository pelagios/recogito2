define([
  'document/annotation/text/selection/highlighter'
], function(TextHighlighter) {

  /**
   * The TEIHighlighter is identical to the standard text Highlighter, except that it
   * overrides the .initPage method.
   */
  var TEIHighlighter = function(rootNode) {

    var initPage = function(annotations) {
          // TODO
        };

    TextHighlighter.apply(this, [ rootNode ]);

    this.initPage = initPage;
  };
  TEIHighlighter.prototype = Object.create(TextHighlighter.prototype);

  return TEIHighlighter;

});
