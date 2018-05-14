define([
  'common/hasEvents',
  'document/annotation/text/relations/edit/relationEditor',
  'document/annotation/text/relations/connection'
], function(HasEvents, RelationEditor, Connection) {

  var RelationsLayer = function(content, svg) {

    var that = this,

        connections = [],

        editor = new RelationEditor(content, svg),

        /** Returns a 'graph node' object { annotation: ..., elements: ... } for the given ID **/
        getNode = function(annotationId) {
          var elements = jQuery('*[data-id="' + annotationId + '"]');
          if (elements.length > 0)
            return {
              annotation: elements[0].annotation,
              elements: elements
            };
        },

        /**
         * Initializes the layer with a list of annotations.
         *
         * Loops through the annotations, disregarding all that don't have relations, and
         * builds connections for the others. (Reminder: a 'connection' is the visual rendition
         * of a relation).
         */
        init = function(annotations) {
          connections = annotations.reduce(function(arr, annotation) {
            if (annotation.relations && annotation.relations.length > 0) {
              // For each annotation that has relations, build the connections
              var connections = annotation.relations.map(function(r) {
                var fromNode = getNode(annotation.annotation_id),
                    toNode = getNode(r.relates_to),

                    // TODO will only work as long as we allow exactly one TAG body
                    label = r.bodies[0].value,

                    connection = new Connection(svg, fromNode, toNode);

                connection.setLabel(label);
                return connection;
              });

              // Attach the relations from this annotations to the global list
              return arr.concat(connections);
            } else {
              return arr;
            }
          }, []);
        },

        /** Show the relations layer **/
        show = function() {
          editor.setEnabled(true); // TODO make dependent on write access rights
          svg.style.display = 'initial';
        },

        /** Hide the relations layer **/
        hide = function() {
          editor.setEnabled(false); // TODO make dependent on write access rights
          svg.style.display = 'none';
        },

        /** Recomputes (and redraws) all connections - called on window resize **/
        recomputeAll = function() {
          connections.forEach(function(connection) {
            connection.recompute();
          });
        };

    jQuery(window).on('resize', recomputeAll);

    // Forward editor events to app
    editor.on('updateRelations', that.forwardEvent('updateRelations'));

    this.init = init;
    this.show = show;
    this.hide = hide;

    HasEvents.apply(this);
  };
  RelationsLayer.prototype = Object.create(HasEvents.prototype);

  return RelationsLayer;

});
