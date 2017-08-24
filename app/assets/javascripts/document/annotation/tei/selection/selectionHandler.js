define([
  'common/config',
  'document/annotation/tei/selection/xpath',
  'document/annotation/text/selection/selectionHandler'
], function(Config, XPath, TextSelectionHandler) {

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
          var getClosestTEIParent = function(node) {
                var parent = node.parentNode;
                if (parent.nodeName.toLowerCase().indexOf('tei-') === 0)
                  return parent;
                else
                  return getClosestTEIParent(parent);
              },

              // Helper to compute char offsets between end of XPATH and given selection bound
              getOffsetFromTo = function(fromNode, toNode, toOffset) {
                var range = rangy.createRange();
                range.setStart(fromNode, 0);
                range.setEnd(toNode, toOffset);
                return range.toString().length;
              },
              startOffset = getOffsetFromTo(
                getClosestTEIParent(selectedRange.startContainer),
                selectedRange.startContainer,
                selectedRange.startOffset),

              endOffset = getOffsetFromTo(
                getClosestTEIParent(selectedRange.endContainer),
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

        rangeToAnnotationStub = function(selectedRange) {
          var startDOMPath = XPath.getXPath(selectedRange.startContainer),
              endDOMPath = XPath.getXPath(selectedRange.endContainer),
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

  return TEISelectionHandler;

});
