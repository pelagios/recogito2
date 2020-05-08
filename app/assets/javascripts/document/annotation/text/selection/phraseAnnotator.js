/**
 * Productivity feature: queries for untagged occurences of a phrase, and automatically
 * prompts the user to re-apply the current annotation.
 */
define([
  'common/utils/annotationUtils',
  'common/config'
], function(AnnotationUtils, Config) {

  var TEXT = 3, // HTML text node type

      // From https://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript/3561711#3561711
      escapeRegExp = function(str) {
        return str.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'); // $& means the whole matched string
      };

  var PhraseAnnotator = function(contentEl, highlighter) {

    var that = this,

        getUnannotatedSegments = function() {
          var unannotatedNodes = jQuery(contentEl).find(':not(.annotation)').addBack().contents();
          return jQuery.grep(unannotatedNodes, function(node) {
            return node.nodeType == TEXT;
          });
        },

        countOccurrences = function(phrase, requireFullWord) {
          // Cf. https://superuser.com/questions/903168/how-should-i-write-a-regex-to-match-a-specific-word
          var splitter = (requireFullWord) ?
            new RegExp('(?:^|\\s)' + escapeRegExp(phrase) + '(?:$|\\s)', 'g') : phrase;

          return getUnannotatedSegments().reduce(function(sum, textNode) {
            var split = textNode.nodeValue.split(splitter);
            return sum + split.length - 1;
          }, 0);
        },

        createSelections = function(annotation, requireFullWord) {
          var quote = AnnotationUtils.getQuote(annotation),

              searchFromOffset = function(text, regex, offset) {
                var substr = text.substring(offset),
                    idx = substr.search(regex);

                if (idx < 0)
                  return idx;
                else
                  return idx + offset + 1;
              },

              createRangesInTextNode = function(textNode) {
                var text = textNode.nodeValue,
                    ranges = [],

                    addRange = function(textNode, from, to) {
                      var range = rangy.createRange();
                      range.setStartAndEnd(textNode, from, to);
                      ranges.push(range);
                    },

                    createForFullWordMatch = function() {
                      var regex = new RegExp('(?:^|\\s)' +
                            escapeRegExp(quote) + '(?:$|\\s)', 'g'),

                          result, idx,

                          len = quote.length;

                      while ((result = regex.exec(text))) {
                        // This index points to the regex match, i.e. the quote
                        // PLUS WHITESPACE - need to take this into account, too
                        idx = result.index + result[0].indexOf(quote);
                        addRange(textNode, idx, idx + len);
                      }
                    },

                    createForAnyMatch = function() {
                      var nextIdx = text.indexOf(quote),
                          cursor;

                      while (nextIdx > -1) {
                        cursor = nextIdx + quote.length;
                        addRange(textNode, nextIdx, cursor);
                        nextIdx = text.indexOf(quote, cursor);
                      }
                    };

                if (requireFullWord)
                  createForFullWordMatch();
                else
                  createForAnyMatch();

                // Sorted bottom-to-top, so we can render them safely (we
                // know they are not overlapping, by definition!)
                return ranges.reverse();
              },

              createSelectionsInTextNode = function(textNode) {
                return createRangesInTextNode(textNode).map(function(range) {
                  var a = {
                        annotates: {
                          document_id: Config.documentId,
                          filepart_id: Config.partId,
                          content_type: Config.contentType
                        },
                        anchor: that.rangeToAnchor(range, textNode, contentEl),

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

  PhraseAnnotator.prototype.rangeToAnchor = function(range, textNode, contentEl) {
    var rangeBefore = rangy.createRange();
    rangeBefore.setStart(contentEl, 0);
    rangeBefore.setEnd(textNode, range.startOffset);
    return 'char-offset:' + rangeBefore.toString().length;
  };

  return PhraseAnnotator;

 });
