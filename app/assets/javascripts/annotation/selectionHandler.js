define(['storage'], function(Storage) {

  var selectionHandler = function(rootNode) {

        /** Trims a range by removing leading and trailing spaces **/
    var trimRange = function(range) {
          var quote = range.toString(),
              leadingSpaces = 0,
              trailingSpaces = 0;

          // Strip & count leading whitespace, adjust range
          while (quote.substring(0, 1) === ' ') {
            leadingSpaces += 1;
            quote = quote.substring(1);
          }

          if (leadingSpaces > 0)
            range.setStart(range.startContainer, range.startOffset + leadingSpaces);

          // Strip & count trailing whitespace, adjust range
          while (quote.substring(quote.length - 1) === ' ') {
            trailingSpaces += 1;
            quote = quote.substring(0, quote.length - 1);
          }

          if (trailingSpaces > 0)
            range.setEnd(range.endContainer, range.endOffset - trailingSpaces);

          return range;
        },

        rangeToAnnotationStub = function(selectedRange) {
          var rangeBefore = rangy.createRange();
          // A helper range from the start of the contentNode to the start of the selection
          rangeBefore.setStart(rootNode, 0);
          rangeBefore.setEnd(selectedRange.startContainer, selectedRange.startOffset);

          return {
            annotates: {
              document: window.config.documentId,
              filepart: window.config.filepartId
            },
            anchor: 'char-offset:' + rangeBefore.toString().length,
            bodies: [
              { type: 'QUOTE', value: selectedRange.toString() },
              { type: 'PLACE' } // TODO this is just a dummy for now
            ]
          };
        },

        onSelect = function(e) {
          var selection = rangy.getSelection(),
              annotation;

          if (!selection.isCollapsed &&
               selection.rangeCount == 1 &&
               selection.getRangeAt(0).toString().trim().length > 0) {

            var selectedRange = trimRange(selection.getRangeAt(0)),
                stub = rangeToAnnotationStub(selectedRange),

                onStoreSuccess = function() {
                  var highlight = document.createElement('SPAN');
                  highlight.className = 'entity PLACE';
                  selectedRange.surroundContents(highlight);
                },

                onStoreError = function(error) {
                  console.log('Error creating annotation');
                  console.log(error);
                };

            Storage.createAnnotation(stub, onStoreSuccess, onStoreError);
          }
        };

    rangy.init();
    jQuery(rootNode).mouseup(onSelect);
  };

  return selectionHandler;

});
