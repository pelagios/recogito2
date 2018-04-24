/**
 * Productivity feature: queries for untagged occurences of a phrase, and automatically
 * prompts the user to re-apply the current annotation.
 */
define([
  'document/annotation/tei/selection/pathUtils',
  'document/annotation/text/selection/phraseAnnotator'
], function(PathUtils, TextPhraseAnnotator) {

  var TEIPhraseAnnotator = function(contentEl, highlighter) {
    this.rootNode = contentEl;
    TextPhraseAnnotator.apply(this, [ contentEl, highlighter ]);
  };
  TEIPhraseAnnotator.prototype = Object.create(TextPhraseAnnotator.prototype);

  TEIPhraseAnnotator.prototype.rangeToAnchor = function(selectedRange, textNode, contentEl) {
    var startDOMPath = PathUtils.getXPath(selectedRange.startContainer),
        endDOMPath = PathUtils.getXPath(selectedRange.endContainer);

    return PathUtils.toTEIPaths(this.rootNode, startDOMPath, endDOMPath, selectedRange);
  };

  return TEIPhraseAnnotator;

 });
