define([
  'common/config',
  'common/hasEvents',
  'document/annotation/text/relations/edit/hoverEmphasis',
  'document/annotation/text/relations/connection',
  'document/annotation/text/relations/tagging/tagPopup'
], function(Config, HasEvents, HoverEmphasis, Relation, TagEditor) {

  var SVG_NS = "http://www.w3.org/2000/svg",

      getAnnotationSpansRecursive = function(element, a) {
        var spans = (a) ? a : [ ],
            parent = element.parentNode;

        spans.push(element);

        if (jQuery(parent).hasClass('annotation'))
          return getAnnotationSpansRecursive(parent, spans);
        else
          return spans;
      },

      toNode = function(e) {
        var t = jQuery(e.target).closest('.annotation'),
            sourceSpan = (t.length > 0) ? t[0] : undefined,
            annotationSpans, annotation, elements;

        if (sourceSpan) {
          annotationSpans = getAnnotationSpansRecursive(sourceSpan);
          annotation = annotationSpans[annotationSpans.length - 1].annotation;
          elements = jQuery('.annotation[data-id="' + annotation.annotation_id + '"]');
          return { annotation: annotation, elements: elements };
        }

      };

  var RelationsLayer = function(content, svg) {

    var that = this,

        contentEl = jQuery(content),

        // Current hover emphasis (regardless of whether we're currently drawing a connection)
        currentHover = false,

        // The connection currently being drawn, if any
        currentRelation = false,

        attachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.addClass('noselect');
          contentEl.on('mousedown', onMousedown);
          contentEl.on('mousemove', onMousemove);
          contentEl.on('mouseup', onMouseup);
          contentEl.on('mouseover', '.annotation', onEnterAnnotation);
          contentEl.on('mouseleave', '.annotation', onLeaveAnnotation);
        },

        detachHandlers = function() {
          // Note that the SVG element is transparent to mouse events
          contentEl.removeClass('noselect');
          contentEl.off('mousedown', onMousedown);
          contentEl.off('mousemove', onMousemove);
          contentEl.off('mouseup', onMouseup);
          contentEl.off('mouseover', '.annotation', onEnterAnnotation);
          contentEl.off('mouseleave', '.annotation', onLeaveAnnotation);
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

        /** Start drawing a new connection line **/
        startNewConnection = function(fromNode) {
          currentRelation = new Relation(svg, fromNode);
          jQuery(document.body).css('cursor', 'none');
          render();
        },

        /** Complete drawing of a new relation **/
        completeConnection = function() {
              // Adds a relation, or replaces an existing one if any exists with the same end node
          var addOrReplaceRelation = function(annotation, relation) {
                if (!annotation.relations) annotation.relations = [];

                var existing = annotation.relations.filter(function(r) {
                  return r.relates_to === relation.relates_to;
                });

                if (existing.length > 0)
                  annotation.relations[annotation.relations.indexOf(existing)] = relation;
                else
                  annotation.relations.push(relation);
              },

              onCancel = function() {
                currentRelation.destroy();
                currentRelation = undefined;
              },

              onSubmit = function(tag) {
                var editor,

                    sourceAnnotation = currentRelation.getStartNode().annotation,

                    relation = {
                      relates_to: currentRelation.getEndNode().annotation.annotation_id,
                      bodies: [{
                        type: 'TAG',
                        last_modified_by: Config.me,
                        value: tag
                      }]
                    };

                currentRelation.setLabel(tag);
                currentRelation.redraw();

                addOrReplaceRelation(sourceAnnotation, relation);
                that.fireEvent('updateRelations', sourceAnnotation);

                currentRelation = undefined;
              };

          currentRelation.attach();

          editor = new TagEditor(contentEl, currentRelation.getMidXY());
          editor.on('cancel', onCancel);
          editor.on('submit', onSubmit);

          jQuery(document.body).css('cursor', 'auto');
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

          if (node)
            if (currentRelation) completeConnection(node);
            else startNewConnection(node);
        },

        onMousemove = function(e) {
          if (currentRelation && !currentRelation.isAttached())
            if (currentHover) currentRelation.dragTo(currentHover.node);
            else currentRelation.dragTo([ e.offsetX, e.offsetY ]);
        },

        /**
         * Note: we want to support both possible drawing modes: click once for start + once for
         * end; or click and hold at the start, drag to end and release.
         */
        onMouseup = function(e) {
          if (currentHover && currentRelation)
            // If this is a different node than the start node, complete the connection
            if (currentHover.annotation !== currentRelation.getStartNode().annotation)
              completeConnection(currentHover.node);
        },

        render = function() {
          if (currentRelation) {
            currentRelation.redraw();
            requestAnimationFrame(render);
          }
        },

        setEnabled = function(enabled) {
          if (enabled)
            attachHandlers();
          else
            detachHandlers();
        };

    this.setEnabled = setEnabled;

    HasEvents.apply(this);
  };
  RelationsLayer.prototype = Object.create(HasEvents.prototype);

  return RelationsLayer;

});
