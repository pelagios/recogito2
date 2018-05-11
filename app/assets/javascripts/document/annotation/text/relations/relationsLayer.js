define([
  'common/hasEvents',
  'document/annotation/text/relations/edit/relationEditor',
  'document/annotation/text/relations/connection'
], function(HasEvents, RelationEditor, Connection) {

  var RelationsViewer = function(content, svg) {

    var that = this,

        editor = new RelationEditor(content, svg),

        connections = [],

        getNode = function(annotationId) {
          var elements = jQuery('*[data-id="' + annotationId + '"]'),
              annotation;

          if (elements.length > 0)
            return {
              annotation: elements[0].annotation,
              elements: elements
            };
        },

        init = function(annotations) {
          connections = annotations.reduce(function(arr, annotation) {
            if (annotation.relations && annotation.relations.length > 0) {

              var connections = annotation.relations.map(function(r) {
                var fromNode = getNode(annotation.annotation_id),
                    toNode = getNode(r.relates_to),

                    // Will work as long as we allow exactly one TAG body
                    label = r.bodies[0].value,

                    connection = new Connection(svg, fromNode, toNode);

                connection.setLabel(label);
                return connection;
              });

              connections.forEach(function(c) { c.redraw(); });
              return arr.concat(connections);
            } else {
              return arr;
            }
          }, []);
        },

        /** Fire up the mouse handlers and show the SVG foreground plate **/
        show = function() {
          // TODO make dependent on write access rights
          editor.setEnabled(true);
          svg.style.display = 'initial';
        },

        /** Clear current connection, all shapes, detach mouse handlers and hide the SVG plate **/
        hide = function() {
          currentRelation = false;

          while (svg.firstChild)
            svg.removeChild(svg.firstChild);

          // TODO make dependent on write access rights
          editor.setEnabled(false);
          svg.style.display = 'none';
        },

        redrawAll = function() {
          connections.forEach(function(connection) {
            connection.recompute();
          });
        };

    jQuery(window).on('resize', redrawAll);

    editor.on('updateRelations', that.forwardEvent('updateRelations'));

    this.show = show;
    this.hide = hide;
    this.init = init;

    HasEvents.apply(this);
  };
  RelationsViewer.prototype = Object.create(HasEvents.prototype);

  return RelationsViewer;

});
