define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'],

  function(Config, AbstractSelectionHandler) {

  var SelectionHandler = function(rootNode, highlighter) {

    var self = this,

        currentSelection = false,

        trimRange = function(range) {
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
              document_id: Config.documentId,
              filepart_id: Config.partId,
              content_type: Config.contentType
            },
            anchor: 'char-offset:' + rangeBefore.toString().length,
            bodies: [
              { type: 'QUOTE', value: selectedRange.toString() }
            ]
          };
        },

        /** Helper that clears the visible selection by 'unwrapping' the created span elements **/
        clearSelection = function() {
          currentSelection = false;
          jQuery.each(jQuery('.selection'), function(idx, el) {
            jQuery(el).contents().unwrap();
          });
          rootNode.normalize();
        },

        /** cf. http://stackoverflow.com/questions/3169786/clear-text-selection-with-javascript **/
        clearNativeSelection = function() {
          if (window.getSelection) {
            if (window.getSelection().empty)
              window.getSelection().empty();
            else if (window.getSelection().removeAllRanges)
              window.getSelection().removeAllRanges();
          } else if (document.selection) {
            document.selection.empty();
          }
        },

        getSelection = function() {
          return currentSelection;
        },

        onMouseup = function(e) {

          // Monitor select of existing annotations via DOM
          // jQuery(container).on('click', '.annotation', editAnnotation);

          var isEventOnEditor = jQuery(e.target).closest('.text-annotation-editor').length > 0,

              // Click happend on an existing annotation span?
              annotationSpan = jQuery(e.target).closest('.annotation'),

              // Or click was part of a new text selection ?
              selection = rangy.getSelection(),
              selectedRange, annotation, bounds, spans;

          // If the mouseup happened on the editor, we'll ignore
          if (isEventOnEditor)
            return;

          // Check for new text selection first - takes precedence over clicked annotation span
          if (!selection.isCollapsed &&
               selection.rangeCount == 1 &&
               selection.getRangeAt(0).toString().trim().length > 0) {

             selectedRange = trimRange(selection.getRangeAt(0));
             annotation = rangeToAnnotationStub(selectedRange);
             bounds = selectedRange.nativeRange.getBoundingClientRect();
             spans = highlighter.wrapRange(selectedRange);
             jQuery.each(spans, function(idx, span) { span.className = 'selection'; });

             clearNativeSelection();

             // A new selection
             currentSelection = {
               isNew      : true,
               annotation : annotation,
               bounds     : bounds,

               // Text-UI specific field - speeds things up a bit
               // in highlighter.convertSelectionToAnnotation
               spans      : spans
             };

             self.fireEvent('select', currentSelection);
          } else if (annotationSpan.length > 0) {
            // Top-most annotation at this span
            annotation = highlighter.getAnnotationsAt(annotationSpan[0])[0];

            // A selection on an existing annotation
            currentSelection = {
              isNew      : false,
              annotation : annotation,
              bounds     : annotationSpan[0].getBoundingClientRect()
            };

            self.fireEvent('select', currentSelection);
          }

          return false;
        };

    jQuery(rootNode).mouseup(onMouseup);

    this.clearSelection = clearSelection;
    this.getSelection = getSelection;

    AbstractSelectionHandler.apply(this);
  };
  SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

  return SelectionHandler;

});
