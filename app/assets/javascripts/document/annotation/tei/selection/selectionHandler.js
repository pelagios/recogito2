define([
  'common/config',
  'document/annotation/tei/selection/xpath',
  'document/annotation/text/selection/selectionHandler'
], function(Config, XPath, TextSelectionHandler) {

  /**
   * The TEISelectionHandler is completely identical to the standard text
   * selection handler, except that it overrides the rangeToAnnotationStub
   * method, so that anchors are expressed using XPath, rather than through char-offset.
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
          var xpath = XPath.getXPath(selectedRange.startContainer),
              teiPath = toTEIPath(xpath) + ':offset=' + selectedRange.startOffset;

          return {
            annotates: {
              document_id: Config.documentId,
              filepart_id: Config.partId,
              content_type: Config.contentType
            },
            anchor: teiPath,
            bodies: [
              { type: 'QUOTE', value: selectedRange.toString() }
            ]
          };
        };

    this.rangeToAnnotationStub = rangeToAnnotationStub;

    TextSelectionHandler.apply(this, [ rootNode, highlighter ]);
  };
  TEISelectionHandler.prototype = Object.create(TextSelectionHandler.prototype);

  return TEISelectionHandler;

});
