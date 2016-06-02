define(['common/helpers/annotationUtils'], function(AnnotationUtils) {

  var TEXT = 3; // HTML DOM node type for text nodes

  var Highlighter = function(rootNode) {

        /** Recursively gets all text nodes inside a given node **/
    var walkTextNodes = function(node, nodeArray) {
          var nodes = (nodeArray) ? nodeArray : [];

          if (node.nodeType === TEXT)
            nodes.push(node);

          node = node.firstChild;

          while(node) {
            walkTextNodes(node, nodes);
            node = node.nextSibling;
          }

          return nodes;
        },

        /**
         * Given a rootNode, this helper gets all text between a given
         * start- and end-node. Basically combines walkTextNodes (above)
         * with a hand-coded dropWhile & takeWhile.
         */
        textNodesBetween = function(startNode, endNode, rootNode) {
          var allTextNodes = walkTextNodes(rootNode),
              nodesBetween = [],
              len = allTextNodes.length,
              take = false,
              n, i;

          for (i=0; i<len; i++) {
            n = allTextNodes[i];

            if (n === endNode)
              take = false;

            if (take)
              nodesBetween.push(n);

            if (n === startNode)
              take = true;
          }

          return nodesBetween;
        },

        /**
         * Given a root node, this method returns the DOM positions (an array of (node, offset)
         * pairs) that correspond to the given character offsets.
         */
        charOffsetToDOMPosition = function(rootNode, charOffsets) {

        },

        /** Shorthand **/
        surround = function(range, css) {
          var wrapper = document.createElement('SPAN');
          wrapper.className = css;
          range.surroundContents(wrapper);
          return wrapper;
        },

        wrapRange = function(range, cssClass) {
          if (range.startContainer === range.endContainer) {
            return [ surround(range, cssClass) ];
          } else {
            // The tricky part - we need to break the range apart and create
            // sub-ranges for each segment
            var nodesBetween =
              textNodesBetween(range.startContainer, range.endContainer, rootNode);

            // Start with start and end nodes
            var startRange = rangy.createRange();
            startRange.selectNodeContents(range.startContainer);
            startRange.setStart(range.startContainer, range.startOffset);
            var startWrapper = surround(startRange, cssClass);

            var endRange = rangy.createRange();
            endRange.selectNode(range.endContainer);
            endRange.setEnd(range.endContainer, range.endOffset);
            var endWrapper = surround(endRange, cssClass);

            // And wrap nodes in between, if any
            var centerWrappers = jQuery.map(nodesBetween.reverse(), function(node) {
              var r = rangy.createRange();
              r.selectNodeContents(node);
              return surround(r, cssClass);
            });

            return [ startWrapper ].concat(centerWrappers,  [ endWrapper ]);
          }
        },

        determineCSSClass = function(annotation) {
          var entityType = AnnotationUtils.getEntityType(annotation),
              statusValues = AnnotationUtils.getStatus(annotation),
              cssClass = (entityType) ? 'annotation ' + entityType.toLowerCase() : 'annotation';

          if (statusValues.length > 0)
            cssClass += ' ' + statusValues.join(' ');

          return cssClass;
        },

        updateStyles = function(annotation, spans) {
          var cssClass = determineCSSClass(annotation);
          jQuery.each(spans, function(idx, span) {
            span.className = cssClass;
          });
        },

        initPage = function(annotations) {
          var textNodes = (function() {
                var start = 0;

                // We only have one text element but, alas, browsers split them
                // up into several nodes
                return jQuery.map(rootNode.childNodes, function(node) {
                  var nodeLength = jQuery(node).text().length,
                      nodeProps = { node: node, start: start, end: start + nodeLength };
                  start += nodeLength;
                  return nodeProps;
                });
              })(),

              intersects = function(a, b) {
                return a.start < b.end && a.end > b.start;
              },

              setNonOverlappingRange = function(range, offset, length) {
                var startNode, startOffest, endNode, endOffset;

                // Compute start-/endnodes and -offsets based on global offset and length
                jQuery.each(textNodes, function(idx, node) {
                  if (offset >= node.start && offset <= node.end) {
                    startNode = node.node;
                    startOffset = offset - node.start;
                  }

                  if ((offset + length) >= node.start && (offset + length) <= node.end) {
                    endNode = node.node;
                    endOffset = offset + length - node.start;
                  }
                });

                // Set the range
                if (startNode === endNode) {
                  range.setStartAndEnd(startNode, startOffset, endOffset);
                } else {
                  range.setStart(startNode, startOffset);
                  range.setEnd(endNode, endOffset);
                }
              },

              classApplier = rangy.createClassApplier('annotation');

          // We're folding over the array, with a 2-sliding window so we can
          // check if this annotation overlaps the previous one
          annotations.reduce(function(previousBounds, annotation) {
            var anchor = parseInt(annotation.anchor.substr(12)),
                quote = AnnotationUtils.getQuote(annotation),
                bounds = { start: anchor, end: anchor + quote.length },
                range = rangy.createRange(),
                spans;

            if (previousBounds && intersects(previousBounds, bounds)) {
              range.selectCharacters(rootNode.childNodes[0], bounds.start, bounds.end);
              spans = wrapRange(range, determineCSSClass(annotation));
            } else {
              // Fast rendering through Rangy's API
              setNonOverlappingRange(range, anchor, quote.length);
              classApplier.applyToRange(range);
              spans = [ range.getNodes()[0].parentElement ];
              updateStyles(annotation, spans);
            }

            // Attach annotation data as payload to the SPANs and set id, if any
            if (spans)
              AnnotationUtils.attachAnnotation(spans, annotation);
            return bounds;
          }, false);
        },

        /**
         *'Mounts' an annotation to the given spans, by applying the according
         * CSS classes, and attaching the annotation object to the elements.
         */
        convertSpansToAnnotation = function(spans, annotation) {
          var anchor = annotation.anchor.substr(12),
              quote = AnnotationUtils.getQuote(annotation);

          updateStyles(annotation, spans);
          AnnotationUtils.attachAnnotation(spans, annotation);
        },

        /**
         * Creates highlight spans for an annotation and mounts it.
         * Returns the created spans for convenience.
         */
        renderAnnotation = function(annotation) {
          var range = rangy.createRange(), spans;

          range.selectCharacters(rootNode.childNodes[0], parseInt(anchor), parseInt(anchor) + quote.length);
          spans = wrapRange(range);
          convertSpansToAnnotation(spans, annotation);
          return spans;
        },

        removeAnnotation = function(annotation) {
          var spans = jQuery('[data-id="' + annotation.annotation_id + '"]');
          jQuery.each(spans, function(idx, span) {
            var el = jQuery(span);
            el.replaceWith(el.contents());
          });
          rootNode.normalize();
        },

        refreshAnnotation = function(annotation) {
          var spans = jQuery('[data-id=' + annotation.annotation_id + ']');
          spans.removeClass();
          spans.addClass(determineCSSClass(annotation));
          return spans;
        },

        /**
         * Returns all annotations this DOM element is enclosed in.
         *
         * Results are sorted by length, shortest first, so that the 'smallest' annotation
         * is the first in the list.
         */
        getAnnotationsAt = function(element, annotation) {
          // Helper to get all annotations in case of multipe nested annotation spans
          var getAnnotationsRecursive = function(element, a) {
                var annotations = (a) ? a : [ ],
                    parent = element.container;

                annotations.push(element.annotation);

                if (jQuery(parent).hasClass('annotation'))
                  return getAnnotationsRecursive(parent, annotations);
                else
                  return annotations;
              },

              sortByQuoteLength = function(annotations) {
                return annotations.sort(function(a, b) {
                  return AnnotationUtils.getQuote(a).length - AnnotationUtils.getQuote(b).length;
                });
              };

          return sortByQuoteLength(getAnnotationsRecursive(element));
        };

    this.convertSpansToAnnotation = convertSpansToAnnotation;
    this.getAnnotationsAt = getAnnotationsAt;
    this.initPage = initPage;
    this.refreshAnnotation = refreshAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.renderAnnotation = renderAnnotation;
    this.wrapRange = wrapRange;
  };

  return Highlighter;

});
