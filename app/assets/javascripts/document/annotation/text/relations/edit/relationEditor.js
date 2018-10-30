define([
  'common/utils/annotationUtils',
  'document/annotation/text/relations/edit/hoverEmphasis',
  'document/annotation/text/relations/connection'
], function(AnnotationUtils, HoverEmphasis, Connection) {

      /**
       * Helper: gets all stacked annotation SPANS for an element.
       *
       * Reminder - annotations can be nested. This helper retrieves the whole stack.
       *
       * <span class="annotation" data-id="annotation-01">
       *   <span class="annotation" data-id="annotation-02">
       *     <span class="annotation" data-id="annotation-03">foo</span>
       *   </span>
       * </span>
       */
  var getAnnotationSpansRecursive = function(element, a) {
        var spans = (a) ? a : [ ],
            parent = element.parentNode;

        spans.push(element);

        if (jQuery(parent).hasClass('annotation'))
          return getAnnotationSpansRecursive(parent, spans);
        else
          return spans;
      },

      /**
       * Returns a 'graph node' object { annotation: ..., elements: ... } for the
       * given mouse event. (Used for mouse over emphasis effect.)
       */
      toNode = function(e) {
        var t = jQuery(e.target).closest('.annotation'),
            sourceSpan = (t.length > 0) ? t[0] : undefined,
            annotationSpans, annotation, elements,

            // Helper to sort annotations by length, so that we can 
            // reliably get the inner-most. Identical to what's happening
            // in highlighter.getAnnotationsAt
            sortByQuoteLengthDesc = function(annotations) {
              return annotations.sort(function(a, b) {
                return AnnotationUtils.getQuote(a).length - AnnotationUtils.getQuote(b).length;
              });
            };

        if (sourceSpan) {
          // All stacked annotation spans
          annotationSpans = getAnnotationSpansRecursive(sourceSpan);

          // Annotation ID from the inner-most span in the stack
          annotation = sortByQuoteLengthDesc(annotationSpans.map(function(span) {
            return span.annotation;
          }))[0];

          // ALL spans for this annotation (not just the hovered one)
          elements = jQuery('.annotation[data-id="' + annotation.annotation_id + '"]');

          return { annotation: annotation, elements: elements };
        }

      };

  var RelationEditor = function(content, svg, tagEditor) {

    var that = this,

        contentEl = jQuery(content),

        currentHover = false,

        currentConnection = false,

        attachHandlers = function() {
          contentEl.addClass('noselect');
          contentEl.on('mousedown', onMousedown);
          contentEl.on('mousemove', onMousemove);
          contentEl.on('mouseup', onMouseup);
          contentEl.on('mouseover', '.annotation', onEnterAnnotation);
          contentEl.on('mouseleave', '.annotation', onLeaveAnnotation);
          jQuery(document).on('keydown', onKeydown);
        },

        detachHandlers = function() {
          contentEl.removeClass('noselect');
          contentEl.off('mousedown', onMousedown);
          contentEl.off('mousemove', onMousemove);
          contentEl.off('mouseup', onMouseup);
          contentEl.off('mouseover', '.annotation', onEnterAnnotation);
          contentEl.off('mouseleave', '.annotation', onLeaveAnnotation);
          jQuery(document).off('keydown', onKeydown);
        },

        /** Drawing code for 'hover emphasis' **/
        hover = function(elements) {
          if (elements) {
            currentHover = new HoverEmphasis(svg, elements);
          } else { // Clear hover
            if (currentHover) currentHover.destroy();
            currentHover = undefined;
          }
        },

        /** Emphasise hovered annotation **/
        onEnterAnnotation = function(e) {
          if (currentHover) hover();
          hover(toNode(e).elements);
        },

        /** Clear hover emphasis **/
        onLeaveAnnotation = function(e) {
          hover();
        },

        /** Starts or creates a new relation **/
        onMousedown = function(e) {
          var node = toNode(e);
          if (node) {
            if (currentConnection) completeConnection(node);
            else startNewConnection(node);
          }
        },

        onMousemove = function(e) {
          if (currentConnection && currentConnection.isFloating()) {
            if (currentHover)  {
              currentConnection.dragTo(currentHover.node);
            } else {
              var offset = contentEl.offset();
              currentConnection.dragTo([ e.pageX - offset.left, e.pageY - offset.top ]);
            }
          }
        },

        /**
         * Note: we want to support both possible drawing modes: click once for start + once for
         * end; or click and hold at the start, drag to end and release.
         */
        onMouseup = function(e) {
          if (currentHover && currentConnection && currentConnection.isFloating()) {
            // If this is a different node than the start node, complete the connection
            if (currentHover.annotation !== currentConnection.getStartAnnotation())
              completeConnection(currentHover.node);
          }
        },

        onKeydown = function(e) {
          if (currentConnection && e.which === 27) { // Escape
            currentConnection.destroy();
            currentConnection = undefined;
            jQuery(document.body).css('cursor', 'auto');
          }
        },

        /** Start drawing a new connection line **/
        startNewConnection = function(fromNode) {
          currentConnection = new Connection(content, svg, fromNode, tagEditor);
          jQuery(document.body).css('cursor', 'none');
          render();
        },

        /** Complete drawing of a new connection **/
        completeConnection = function() {
          currentConnection.fix();
          currentConnection.editRelation();
          currentConnection = undefined;
          jQuery(document.body).css('cursor', 'auto');
        },

        render = function() {
          if (currentConnection) {
            currentConnection.redraw();
            requestAnimationFrame(render);
          }
        },

        setEnabled = function(enabled) {
          if (enabled) {
            attachHandlers();
          } else {
            detachHandlers();
            jQuery(document.body).css('cursor', 'auto');
            if (currentConnection) {
              currentConnection.destroy();
              currentConnection = undefined;
            }
          }
        },

        recomputeConnection = function() {
          if (currentConnection) currentConnection.recompute();
        };

    jQuery(window).on('resize', recomputeConnection);

    this.setEnabled = setEnabled;
  };

  return RelationEditor;

});
