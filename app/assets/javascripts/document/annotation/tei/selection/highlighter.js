define([
  'common/utils/annotationUtils',
  'document/annotation/text/selection/highlighter'
], function(AnnotationUtils, TextHighlighter) {

  /**
   * The TEIHighlighter is identical to the standard text Highlighter, except that it
   * overrides the .initPage method.
   */
  var TEIHighlighter = function(rootNode) {

    var self = this,

        getDOMPositions = function(anchor, quote) {
          var offsetIdx = anchor.indexOf(':offset'),

              normalized =
                anchor.substring(0, offsetIdx).replace(/\//g, '/tei-');

              startNode = document.evaluate(normalized.substring(1),
                rootNode, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

              startOffset = parseInt(anchor.substring(offsetIdx + 8));

          return [ { node: startNode, offset: startOffset } ];
        },

        /** Only called from outside, so we can override directly in here **/
        initPage = function(annotations) {
          annotations.forEach(function(annotation) {
            var quote = AnnotationUtils.getQuote(annotation),
                positions = getDOMPositions(annotation.anchor, quote),
                range = rangy.createRange(),
                spans;

            console.log(positions[0]);

            // range.setStart(positions[0].node, positions[0].offset);
            // range.setEnd(positions[1].node, positions[1].offset);

            /*
            spans = self.wrapRange(range);

            self.updateStyles(annotation, spans);
            self.bindToElements(annotation, spans);
            */
          });
        };

    TextHighlighter.apply(this, [ rootNode ]);

    this.initPage = initPage;
  };
  TEIHighlighter.prototype = Object.create(TextHighlighter.prototype);

  return TEIHighlighter;

});
