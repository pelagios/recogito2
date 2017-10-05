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

        getDOMPosition = function(path) {
          var offsetIdx = path.indexOf('::'),

              // CETEIcean-specific: prefix all path elements with 'tei-'!
              normalized = path.substring(0, offsetIdx).replace(/\//g, '/tei-'),

              parentNode = document.evaluate(normalized.substring(1),
                rootNode, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue,

              node = parentNode.firstChild,

              offset = parseInt(path.substring(offsetIdx + 2)),

              reanchor = function() {
                var it = document.createNodeIterator(parentNode, NodeFilter.SHOW_TEXT),
                    currentNode = it.nextNode(),
                    run = true;

                do {
                  if (currentNode.length < offset) {
                    offset -= currentNode.length;
                  } else {
                    node = currentNode;
                    run = false;
                  }
                  currentNode = it.nextNode();
                } while (currentNode && run);
              };

          if (!node.length || offset > node.length) reanchor();

          return { node: node, offset: offset };
        },

        /** Only called from outside, so we can override directly in here **/
        initPage = function(annotations) {
          annotations.forEach(function(annotation) {
            var quote = AnnotationUtils.getQuote(annotation),

                paths = annotation.anchor.split(';'),

                fromPath = paths.find(function(p) {
                  return p.indexOf('from=') === 0;
                }).substring(5),

                toPath = paths.find(function(p) {
                  return p.indexOf('to=') === 0;
                }).substring(3),

                fromPosition = getDOMPosition(fromPath),
                toPosition = getDOMPosition(toPath),

                range = rangy.createRange(), spans;

            range.setStart(fromPosition.node, fromPosition.offset);
            range.setEnd(toPosition.node, toPosition.offset);

            spans = self.wrapRange(range);
            self.updateStyles(annotation, spans);
            self.bindToElements(annotation, spans);
          });
        };

    TextHighlighter.apply(this, [ rootNode ]);

    this.initPage = initPage;
  };
  TEIHighlighter.prototype = Object.create(TextHighlighter.prototype);

  return TEIHighlighter;

});
