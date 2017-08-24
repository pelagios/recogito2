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
              // Computes the char offset from the end of the XPATH to the given DOM node/offset
          var getOffsetTo = function(node, offset) {
                var range = rangy.createRange();
                range.setStart(selectedRange.startContainer.parentNode, 0);
                range.setEnd(node, offset);
                return range.toString().length;
              },

              startOffset = getOffsetTo(selectedRange.startContainer, selectedRange.startOffset),
              endOffset = getOffsetTo(selectedRange.endContainer, selectedRange.endOffset),

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
