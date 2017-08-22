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

    var rangeToAnnotationStub = function(selectedRange) {
          var rangeBefore = rangy.createRange();
          // A helper range from the start of the contentNode to the start of the selection
          rangeBefore.setStart(rootNode, 0);
          rangeBefore.setEnd(selectedRange.startContainer, selectedRange.startOffset);

          // TODO use XPath expression instead of char-offset

          return {
            annotates: {
              document_id: Config.documentId,
              filepart_id: Config.partId,
              content_type: Config.contentType
            },
            anchor: 'char-offset:' + rangeBefore.toString().length,
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
