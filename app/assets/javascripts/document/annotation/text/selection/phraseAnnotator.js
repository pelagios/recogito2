/**
 * Productivity feature: queries for untagged occurences of a phrase, and automatically
 * prompts the user to re-apply the current annotation.
 */
define([
  'common/utils/annotationUtils',
  'common/config'
], function(AnnotationUtils, Config) {

  var TEXT = 3; // HTML text node type

  var PhraseAnnotator = function(contentEl, highlighter) {

    var getUnannotatedSegments = function() {
          return jQuery.grep(jQuery(contentEl).contents(), function(node) {
            return node.nodeType == TEXT;
          });
        },

        countOccurrences = function(phrase) {
          return getUnannotatedSegments().reduce(function(sum, textNode) {
            var split = textNode.nodeValue.split(phrase);
            return sum + split.length - 1;
          }, 0);
        },

        createSelections = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),

              createRangesInTextNode = function(textNode) {
                var text = textNode.nodeValue,
                    nextIdx = text.indexOf(quote),
                    ranges = [],
                    range, cursor;

                while (nextIdx > -1) {
                  cursor = nextIdx + quote.length;

                  range = rangy.createRange();
                  range.setStartAndEnd(textNode, nextIdx, cursor);
                  ranges.push(range);

                  nextIdx = text.indexOf(quote, cursor);
                }

                // Sorted bottom-to-top, so we can render them safely (we
                // know they are not overlapping, by definition!)
                return ranges.reverse();
              },

              createSelectionsInTextNode = function(textNode) {
                var ranges = createRangesInTextNode(textNode);

                return jQuery.map(ranges, function(range) {
                  var computeOffset = function() {
                        var rangeBefore = rangy.createRange();
                        rangeBefore.setStart(contentEl, 0);
                        rangeBefore.setEnd(textNode, range.startOffset);

                        return rangeBefore.toString().length;
                      },

                      a = {
                        annotates: {
                          document_id: Config.documentId,
                          filepart_id: Config.partId,
                          content_type: Config.contentType
                        },
                        anchor: 'char-offset:' + computeOffset(),

                        // Clone bodies
                        bodies: jQuery.map(annotation.bodies, function(body) {
                          return jQuery.extend({}, body);
                        })
                      },

                      bounds = range.nativeRange.getBoundingClientRect(),

                      spans = highlighter.wrapRange(range),

                      selection = {
                        isNew: true,
                        annotation: a,
                        bounds: bounds,
                        spans: spans
                      };

                  highlighter.convertSelectionToAnnotation(selection);
                  return selection;
                });
              };

          return getUnannotatedSegments().reduce(function(selections, textNode) {
            return selections.concat(createSelectionsInTextNode(textNode));
          }, []);
        };

    this.countOccurrences = countOccurrences;
    this.createSelections = createSelections;

  };

  return PhraseAnnotator;

 });
