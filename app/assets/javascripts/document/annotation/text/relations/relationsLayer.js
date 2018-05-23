define([
  'common/config',
  'common/hasEvents',
  'document/annotation/text/relations/edit/relationEditor',
  'document/annotation/text/relations/tagging/tagVocabulary',
  'document/annotation/text/relations/connection'
], function(Config, HasEvents, RelationEditor, Vocabulary, Connection) {

  var RelationsLayer = function(content, svg) {

    var that = this,

        connections = [],

        editor = (Config.writeAccess) ? new RelationEditor(content, svg) : undefined,

        /**
         * Initializes the layer with a list of annotations.
         *
         * Loops through the annotations, disregarding all that don't have relations, and
         * builds connections for the others. (Reminder: a 'connection' is the visual rendition
         * of a relation).
         */
        init = function(annotations) {
          if (!Config.writeAccess) jQuery(svg).addClass('readonly');

          connections = annotations.reduce(function(arr, annotation) {
            if (annotation.relations && annotation.relations.length > 0) {
              // For each annotation that has relations, build the corresponding connections...
              var connections = annotation.relations.map(function(r) {
                var c = new Connection(content, svg, annotation, r);
                if (Config.writeAccess) c.on('update', that.forwardEvent('updateRelations'));
                return c;
              });

              // Attach the relations from this annotations to the global list
              return arr.concat(connections);
            } else {
              return arr;
            }
          }, []);

          Vocabulary.init(annotations);
        },

        /** Show the relations layer **/
        show = function() {
          if (editor) editor.setEnabled(true);
          svg.style.display = 'initial';
        },

        /** Hide the relations layer **/
        hide = function() {
          if (editor) editor.setEnabled(false);
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
          if (optNewConnection)
            connections.push(optNewConnection);

          that.fireEvent('updateRelations', annotation);
        };

    jQuery(window).on('resize', recomputeAll);

    if (editor) editor.on('updateRelations', onUpdateRelations);

    this.init = init;
    this.show = show;
    this.hide = hide;
    this.deleteRelationsTo = deleteRelationsTo;

    HasEvents.apply(this);
  };
  RelationsLayer.prototype = Object.create(HasEvents.prototype);

  return RelationsLayer;

});
