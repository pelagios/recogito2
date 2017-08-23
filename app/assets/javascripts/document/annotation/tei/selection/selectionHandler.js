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
        toTEIPath = function(domPath) {
          return '/' + domPath.slice(domPath.indexOf(pathStart) + 1).join('/').replace(/tei-/g, '');
        },

        rangeToAnnotationStub = function(selectedRange) {
          var startDOMPath = XPath.getXPath(selectedRange.startContainer),
              startTEIPath = toTEIPath(startDOMPath) + '::' + selectedRange.startOffset,

              endDOMPath = XPath.getXPath(selectedRange.endContainer),
              endTEIPath = toTEIPath(endDOMPath) + '::' + selectedRange.endOffset;

          return {
            annotates: {
              document_id: Config.documentId,
              filepart_id: Config.partId,
              content_type: Config.contentType
            },
            anchor: 'from=' + startTEIPath + ';to=' + endTEIPath,
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
