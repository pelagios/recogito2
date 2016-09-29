/**
 * Productivity feature: queries for untagged occurences of the toponym and automatically annotates.
 *
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
