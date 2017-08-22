define([
  'document/annotation/text/selection/highlighter'
], function(TextHighlighter) {

  /**
   * The TEIHighlighter is completely identical to the standard text
   * Highlighter (for now).
   */
  var TEIHighlighter = function(rootNode) {
    TextHighlighter.apply(this, [ rootNode ]);
  };
  TEIHighlighter.prototype = Object.create(TextHighlighter.prototype);

  return TEIHighlighter;

});
