/**
 * Productivity feature: queries for untagged occurences of a phrase, and automatically
 * prompts the user to re-apply the current annotation.
 */
define([
  'common/utils/annotationUtils',
  'common/config'
], function(AnnotationUtils, Config) {

  var TEXT = 3; // HTML text node type

  var PhraseAnnotator = function(contentEl) {

    var reapply = function(annotation) {
          var quote = AnnotationUtils.getQuote(annotation),

              segmentsWithOccurrences = jQuery.grep(jQuery(contentEl).contents(), function(node) {
                if (node.nodeType == TEXT)  { // TEXT
                  var text = node.nodeValue,
                      startIdx = text.indexOf(quote);

                  return startIdx > -1;
                }
              }),

              convertOccurrences = function(textNodes) {
                return jQuery.map(textNodes, function(textNode) {
                  var text = jQuery(textNode).text(),
                      rangeBefore = rangy.createRange(),
                      offset;

                  rangeBefore.setStart(contentEl, 0);
                  rangeBefore.setEnd(textNode, text.indexOf(quote));

                  offset = rangeBefore.toString().length;

                  return {
                    annotates: {
                      document_id: Config.documentId,
                      filepart_id: Config.partId,
                      content_type: Config.contentType
                    },
                    anchor: 'char-offset:' + rangeBefore.toString().length,
                    bodies: annotation.bodies.slice()
                  };
                });
              };

          if (segmentsWithOccurrences.length > 0)
            return convertOccurrences(segmentsWithOccurrences);
        };

    this.reapply = reapply;

  };

  return PhraseAnnotator;

 });

/*
recogito.TextAnnotationUI.prototype.batchAnnotate = function(toponym) {
  var self = this,
      textDiv = document.getElementById('text');

  // Loop through all nodes in the #text DIV...
  var untagged = $.grep($(textDiv).contents(), function(node) {
    // ...and count direct text children that contain the toponym
    if (node.nodeType == 3) {
      var line = $(node).text();
      var startIdx = line.indexOf(' ' + toponym);
      if (startIdx > -1) {
        if (startIdx + toponym.length == line.length) {
          // Toponym at end of line -> Ok
          return true;
        } else {
          var nextChar = line.substr(startIdx + toponym.length + 1, 1);
          return ([' ', '.', ',', ';'].indexOf(nextChar) > -1);
        }
      }
    }
  });

  if (untagged.length > 0) {
    var doBatch = confirm(
      'There are at least ' + untagged.length + ' ' +
      'more un-tagged occurrences of "' + toponym + '". ' +
      'Do you want me to tag them too?');

    if (doBatch) {
      $.each(untagged, function(idx, textNode) {
        var text = $(textNode).text();
        var selectedRange = rangy.createRange();
        selectedRange.setStart(textNode, text.indexOf(toponym));
        selectedRange.setEnd(textNode, text.indexOf(toponym) + toponym.length);

        var ranges = self.normalizeSelection(textDiv, selectedRange);
        self.createAnnotation(false, false, toponym, 0, 0, ranges.offset, ranges.selectedRange, true);
      });
    }
  }
};


*/
