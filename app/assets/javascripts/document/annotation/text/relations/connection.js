/** A connection is an on-screen representation of a relation **/
define([
  'common/config',
  'common/hasEvents',
  'document/annotation/text/relations/bounds',
  'document/annotation/text/relations/drawing',
  'document/annotation/text/relations/edit/tagEditor',
  'document/annotation/text/relations/tagging/tagHandle'
], function(Config, HasEvents, Bounds, Draw, TagEditor, TagHandle) {

      /** Helper to add or replace a relation within an annotation **/
  var addOrReplaceRelation = function(annotation, relation) {
        if (!annotation.relations) annotation.relations = [];

        var existing = annotation.relations.find(function(r) {
          return r.relates_to === relation.relates_to;
        });

        if (existing)
          annotation.relations[annotation.relations.indexOf(existing)] = relation;
        else
          annotation.relations.push(relation);
      },

      removeRelation = function(annotation, relatesToId) {
        if (annotation.relations) {
          var toRemove = annotation.relations.find(function(r) {
                return r.relates_to === relatesToId;
              }),

              idxToRemove = (toRemove) ? annotation.relations.indexOf(toRemove) : -1;

          if (idxToRemove > -1)
            annotation.relations.splice(idxToRemove, 1);

          if (annotation.relations.length === 0)
            delete annotation.relations;
        }
      },

      /** Returns a 'graph node' object { annotation: ..., elements: ... } for the given ID **/
      getNode = function(annotationId) {
        var elements = jQuery('*[data-id="' + annotationId + '"]');
        if (elements.length > 0)
          return {
            annotation: elements[0].annotation,
            elements: elements
          };
      };

  var Connection = function(contentEl, svgEl, nodeOrAnnotation, opt_relation) {

    var that = this,

        svg = jQuery(svgEl), // shorthand

        // { annotation: ..., elements: ... }
        fromNode, toNode,

        // A note on bounds: remember that selection bounds are useless after scrolling or
        // resize. They represent viewport bounds at the time of selection. Therefore, we
        // store document offsets bounds here instead
        fromBounds, toBounds,

        // Flag indicating whether the relation is still drawing (floating) or not
        floating,

        handle, arrow,

        // [x,y] array or node object
        currentEnd,

        initHandle = function(label) {
          if (handle) handle.destroy();
          handle = new TagHandle(label, svgEl);
          handle.on('click', editRelation); // TODO make dependent on access permissions
          redraw();
        },

        /** Initializes a floating connection from a start node **/
        initFromStartNode = function() {
          fromNode = nodeOrAnnotation;
          fromBounds = new Bounds(fromNode.elements, svg);
          floating = true;
        },

        /** Initializes a fixed connection from a relation **/
        initFromRelation = function() {
          fromNode = getNode(nodeOrAnnotation.annotation_id);
          fromBounds = new Bounds(fromNode.elements, svg);

          toNode = getNode(opt_relation.relates_to);
          toBounds = new Bounds(toNode.elements, svg);

          currentEnd = toNode;

          // TODO will only work as long as we allow exactly one TAG body
          initHandle(opt_relation.bodies[0].value);

          floating = false;
        },

        // SVG elements
        path     = document.createElementNS(Draw.SVG_NAMESPACE, 'path'),
        startDot = document.createElementNS(Draw.SVG_NAMESPACE, 'circle'),
        endDot   = document.createElementNS(Draw.SVG_NAMESPACE, 'circle'),

        // Current middle-of-the-path coordinates [x,y]
        currentMidXY,

        /** Moves the end of a floating connection to the given [x,y] or node **/
        dragTo = function(xyOrNode) {
          if (floating) {
            currentEnd = xyOrNode;
            if (xyOrNode.elements) {
              toNode = xyOrNode;
              toBounds = new Bounds(xyOrNode.elements, svg);
            }
          }
        },

        /** Fixes the end of the connection to the current end node, if it is floating **/
        fix = function() {
          if (currentEnd.elements) floating = false;
        },

        isFloating = function() {
          return floating;
        },

        /** Returns the current end [x,y] coords **/
        getEndXY = function() {
          if (currentEnd instanceof Array)
            return currentEnd;
          else
            return (fromBounds.top > toBounds.top) ?
              toBounds.getBottomHandleXY() : toBounds.getTopHandleXY();
        },

        /** Redraws the connection **/
        redraw = function() {
          if (currentEnd) {
            var end = getEndXY(),

                startsAtTop = end[1] <= (fromBounds.top + fromBounds.height / 2),

                start = (startsAtTop) ?
                  fromBounds.getTopHandleXY() : fromBounds.getBottomHandleXY(),

                deltaX = end[0] - start[0],
                deltaY = end[1] - start[1],

                half = (Math.abs(deltaX) + Math.abs(deltaY)) / 2, // Half of length, for middot pos computation
                midX = (half > Math.abs(deltaX)) ? start[0] + deltaX : start[0] + half * Math.sign(deltaX),
                midY, // computed later

                orientation = (half > Math.abs(deltaX)) ?
                  (deltaY > 0) ? 'down' : 'up' :
                  (deltaX > 0) ? 'right' : 'left',

                d = Draw.LINE_DISTANCE - Draw.BORDER_RADIUS, // Shorthand: vertical straight line length

                // Path that starts at the top edge of the annotation highlight
                compileBottomPath = function() {
                  var arc1 = (deltaX > 0) ? Draw.ARC_9CC : Draw.ARC_3CW,
                      arc2 = (deltaX > 0) ? Draw.ARC_0CW : Draw.ARC_0CC;

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] + half - Math.abs(deltaX) + Draw.LINE_DISTANCE :
                    start[1] + Draw.LINE_DISTANCE;

                  return 'M' + start[0] +
                         ' ' + start[1] +
                         'v' + d +
                         arc1 +
                         'h' + (deltaX - 2 * Math.sign(deltaX) * Draw.BORDER_RADIUS) +
                         arc2 +
                         'V' + end[1];
                },

                // Path that starts at the bottom edge of the annotation highlight
                compileTopPath = function() {
                  var arc1 = (deltaX > 0) ? Draw.ARC_9CW : Draw.ARC_3CC,
                      arc2 = (deltaX > 0) ?
                        (deltaY >= 0) ? Draw.ARC_0CW : Draw.ARC_6CC :
                        (deltaY >= 0) ? Draw.ARC_0CC : Draw.ARC_6CW;

                  midY = (half > Math.abs(deltaX)) ?
                    start[1] - (half - Math.abs(deltaX)) - Draw.LINE_DISTANCE :
                    start[1] - Draw.LINE_DISTANCE;

                  return 'M' + start[0] +
                         ' ' + start[1] +
                         'v-' + (Draw.LINE_DISTANCE - Draw.BORDER_RADIUS) +
                         arc1 +
                         'h' + (deltaX - 2 * Math.sign(deltaX) * Draw.BORDER_RADIUS) +
                         arc2 +
                         'V' + end[1];
                };

            startDot.setAttribute('cx', start[0]);
            startDot.setAttribute('cy', start[1]);
            startDot.setAttribute('r', 2);
            startDot.setAttribute('class', 'start');

            endDot.setAttribute('cx', end[0]);
            endDot.setAttribute('cy', end[1]);
            endDot.setAttribute('r', 2);
            endDot.setAttribute('class', 'end');

            if (startsAtTop) path.setAttribute('d', compileTopPath());
            else path.setAttribute('d', compileBottomPath());
            path.setAttribute('class', 'connection');

            currentMidXY = [ midX, midY ];

            if (handle)
              handle.setPosition(currentMidXY, orientation);
          }
        },

        /**
         * Redraws the current connection, but additionally forces a recompute of the
         * start and end coordinates. This is only needed if the relative position of the
         * annotation highlights has changed after a window resize.
         */
        recompute = function() {
          fromBounds = new Bounds(fromNode.elements, svg);
          if (currentEnd && currentEnd.elements)
            toBounds = new Bounds(currentEnd.elements, svg);
          redraw();
        },

        /** Opens the tag editor **/
        editRelation = function() {
          var label = (handle) ? handle.getLabel : undefined,

              editor = new TagEditor(contentEl, currentMidXY, label),

              sourceAnnotation = fromNode.annotation, // shorthand

              onSubmit = function(tag) {
                initHandle(tag);
                addOrReplaceRelation(sourceAnnotation, getRelation());
                that.fireEvent('update', sourceAnnotation);
              },

              onDelete = function() {
                destroy();
                removeRelation(sourceAnnotation, toNode.annotation.annotation_id);
                that.fireEvent('update', sourceAnnotation);
              },

              onCancel = function() {
                if (!handle) // New relation - delete connection on cancel
                  destroy();
                that.fireEvent('cancel');
              };

          editor.on('submit', onSubmit);
          editor.on('delete', onDelete);
          editor.on('cancel', onCancel);
        },

        getRelation = function() {
          return {
            relates_to: toNode.annotation.annotation_id,
            bodies: [{
              type: 'TAG',
              last_modified_by: Config.me,
              value: handle.getLabel()
            }]
          };
        },

        getStartAnnotation = function() {
          return fromNode.annotation;
        },

        getEndAnnotation = function() {
          if (toNode) return toNode.annotation;
        },

        destroy = function() {
          svgEl.removeChild(path);
          svgEl.removeChild(startDot);
          svgEl.removeChild(endDot);
          if (handle) handle.destroy();
        };

    svgEl.appendChild(path);
    svgEl.appendChild(startDot);
    svgEl.appendChild(endDot);

    if (nodeOrAnnotation.annotation_id)
      initFromRelation();
    else
      initFromStartNode();

    redraw();

    this.dragTo = dragTo;
    this.fix = fix;
    this.isFloating = isFloating;
    this.redraw = redraw;
    this.recompute = recompute;

    this.editRelation = editRelation;
    this.getStartAnnotation = getStartAnnotation;
    this.getEndAnnotation = getEndAnnotation;

    this.destroy = destroy;

    HasEvents.apply(this);
  };
  Connection.prototype = Object.create(HasEvents.prototype);

  return Connection;

});
