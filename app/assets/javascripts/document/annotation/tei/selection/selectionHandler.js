define([
  'common/config',
  'document/annotation/tei/selection/pathUtils',
  'document/annotation/text/selection/selectionHandler'
], function(Config, PathUtils, TextSelectionHandler) {

  /**
   * The TEISelectionHandler is identical to the standard text SelectionHandler, except
   * that it overrides the .rangeToAnnotationStub method, so that anchors are expressed
   * using XPath, rather than through char-offset.
   */
  var TEISelectionHandler = function(rootNode, highlighter) {

        /** XPath segment where the TEI document starts **/
    var pathStart = rootNode.nodeName.toLowerCase() + "[@id='" + rootNode.id + "']",

        /**
         * Helper that transforms the CETEIcean-specific DOM XPath to a
         * normalized XPath with the TEI document only.
         */
        toTEIPaths = function(startPath, endPath, selectedRange) {
              // For a given node, returns the closest parent that is a TEI element
          var getClosestTEINode = function(node) {
                if (node.nodeName.toLowerCase().indexOf('tei-') === 0)
                  return node;
                else
                  return getClosestTEINode(node.parentNode);
              },

              // Helper to compute char offsets between end of XPath and given selection bound
              getOffsetFromTo = function(fromNode, toNode, toOffset) {
                var range = rangy.createRange();
                range.setStart(fromNode, 0);
                range.setEnd(toNode, toOffset);
                return range.toString().length;
              },
              startOffset = getOffsetFromTo(
                getClosestTEINode(selectedRange.startContainer),
                selectedRange.startContainer,
                selectedRange.startOffset),

              endOffset = getOffsetFromTo(
                getClosestTEINode(selectedRange.endContainer),
                selectedRange.endContainer,
                selectedRange.endOffset),

              // Removes all refs to non-TEI nodes (i.e. those added by the Recogito view)
              fixPath = function(path) {
                return path.slice(path.indexOf(pathStart) + 1).reduce(function(xpath, p) {
                         if (p.indexOf('tei-') === 0)
                           return xpath + '/' + p.substring(4);
                         else
                           return xpath;
                       }, '');
              },

              startTEIPath = fixPath(startPath) + '::' + startOffset,
              endTEIPath = fixPath(endPath) + '::' + endOffset;

          return 'from=' + startTEIPath + ';to=' + endTEIPath;
        },

        /** Only called from outside, so we can override directly in here **/
        rangeToAnnotationStub = function(selectedRange) {
          var startDOMPath = PathUtils.getXPath(selectedRange.startContainer),
              endDOMPath = PathUtils.getXPath(selectedRange.endContainer),
              teiPaths = toTEIPaths(startDOMPath, endDOMPath, selectedRange);

          return {
            annotates: {
              document_id: Config.documentId,
              filepart_id: Config.partId,
              content_type: Config.contentType
            },
            anchor: teiPaths,
            bodies: [
              { type: 'QUOTE', value: selectedRange.toString() }
            ]
          };
        };

    TextSelectionHandler.apply(this, [ rootNode, highlighter ]);

    this.rangeToAnnotationStub = rangeToAnnotationStub;
  };
  TEISelectionHandler.prototype = Object.create(TextSelectionHandler.prototype);

  /**
   * Trimming the range is a little more complex in the TEI case, since it's not sufficient
   * to ajust the offsets. Trimming might cross element boundaries, so we also need to adjust
   * the anchor node.
   */
  TEISelectionHandler.prototype.trimRange = function(range) {
    var fixRangeEnd = function() {
          var isNonEmptyText = function(node) {
                if (node.nodeType === 3) // It's a text node
                  return node.nodeValue.trim().length > 0;
                else
                  return false;
              },

              start = range.startContainer, // shorthand
              end = range.endContainer, // shorthand
              commonAncestor = PathUtils.getCommonAncestor(start, end),
              it = document.createNodeIterator(commonAncestor, NodeFilter.SHOW_ALL);

          // Bit quirky, but no other way: find the end node, and then trace back
          // to the nearest non-empty text neighbour
          do { current = it.nextNode(); } while (current !== end);
          do { current = it.previousNode(); } while (!isNonEmptyText(current));

          range.setEnd(current, current.nodeValue.length);
        };

    // Range might end in a non-text node. In this case, trim the range to
    // the nearest previous, non-empty text node. (For some weird reason, this
    // never seems to happen with the start node. Just seems to be the way browser
    // selection works...)
    if (range.endContainer.nodeType !== 3)
      fixRangeEnd();

    return TextSelectionHandler.prototype.trimRange.call(this, range);
  };

  return TEISelectionHandler;

});
