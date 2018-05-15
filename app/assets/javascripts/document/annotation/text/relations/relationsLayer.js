define([
  'common/hasEvents',
  'document/annotation/text/relations/edit/relationEditor',
  'document/annotation/text/relations/connection'
], function(HasEvents, RelationEditor, Connection) {

  var RelationsLayer = function(content, svg) {

    var that = this,

        connections = [],

        editor = new RelationEditor(content, svg),

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
              // For each annotation that has relations, build the corresponding connections...
              var connections = annotation.relations.map(function(r) {
                var c = new Connection(content, svg, annotation, r);
                c.on('update', that.forwardEvent('updateRelations'));
                return c;
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
        },

        /**
         * Deletes all relations pointing to the given annotation ID (i.e. in case a user
         * deletes the annotation in the UI).
         */
        deleteRelationsTo = function(annotationId) {
          // Sort of side-effect-ugly but well...
          connections = connections.filter(function(conn) {
            var isAffected = conn.getEndAnnotation().annotation_id === annotationId;
            if (isAffected) conn.destroy();
            return !isAffected;
          });
        },

        onUpdateRelations = function(annotation, optNewConnection) {
          if (optNewConnection) {
            optNewConnection.on('click', onConnectionClicked);
            connections.push(optNewConnection);
          }

          that.fireEvent('updateRelations', annotation);
        };

    jQuery(window).on('resize', recomputeAll);

    editor.on('updateRelations', onUpdateRelations);

    this.init = init;
    this.show = show;
    this.hide = hide;
    this.deleteRelationsTo = deleteRelationsTo;

    HasEvents.apply(this);
  };
  RelationsLayer.prototype = Object.create(HasEvents.prototype);

  return RelationsLayer;

});
