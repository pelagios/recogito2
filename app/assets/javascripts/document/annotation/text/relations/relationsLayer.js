define([
  'common/config',
  'common/hasEvents',
  'document/annotation/text/relations/edit/relationEditor',
  'document/annotation/text/relations/edit/tagPopup',
  'document/annotation/text/relations/tagging/tagVocabulary',
  'document/annotation/text/relations/connection'
], function(Config, HasEvents, RelationEditor, TagPopup, Vocabulary, Connection) {

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
    };

  var RelationsLayer = function(content, svg) {

    var that = this,

        connections = [],

        tagPopup = new TagPopup(content),

        editor = (Config.writeAccess) ? new RelationEditor(content, svg, tagPopup) : undefined,

        onUpdateConnection = function(connection) {
          var startAnnotation = connection.getStartAnnotation();
          addOrReplaceRelation(startAnnotation, connection.getRelation());
          if (!connection.isStored()) connections.push(connection);
          connection.setStored();
          that.fireEvent('updateRelations', startAnnotation);
        },

        onDeleteConnection = function(connection) {
          var startAnnotation = connection.getStartAnnotation(),
              endAnnotation = connection.getEndAnnotation();

          removeRelation(startAnnotation, endAnnotation.annotation_id);

          // TODO remove from connections array
          that.fireEvent('updateRelations', startAnnotation);
        },

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
            try {
              if (annotation.relations && annotation.relations.length > 0) {
                // For each annotation that has relations, build the corresponding connections...
                var connections = annotation.relations.map(function(r) {
                  var c = new Connection(content, svg, annotation, tagPopup, r);
                  // if (Config.writeAccess) c.on('update', that.forwardEvent('updateRelations'));
                  return c;
                });

                // Attach the relations from this annotations to the global list
                return arr.concat(connections);
              } else {
                return arr;
              }
            } catch (error) {
              console.log(annotation.annotation_id);
              return arr;
            }
          }, []);

          Vocabulary.init(annotations);

          if (Config.writeAccess) {
            tagPopup.on('submit', onUpdateConnection);
            tagPopup.on('delete', onDeleteConnection);
          }
        },

        show = function() {
          svg.style.display = 'initial';
        },

        hide = function() {
          setDrawingEnabled(false);
          svg.style.display = 'none';
        },

        setDrawingEnabled = function(enabled) {
          if (editor)  {
            editor.setEnabled(enabled);
            if (!enabled)
              connections.forEach(function(conn) {
                conn.stopEditing();
              });
          }
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
        };

    jQuery(window).on('resize', recomputeAll);

    this.init = init;
    this.show = show;
    this.hide = hide;
    this.setDrawingEnabled = setDrawingEnabled;
    this.deleteRelationsTo = deleteRelationsTo;

    HasEvents.apply(this);
  };
  RelationsLayer.prototype = Object.create(HasEvents.prototype);

  return RelationsLayer;

});
