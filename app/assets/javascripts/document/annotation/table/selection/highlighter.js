define([
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/annotation/common/selection/abstractHighlighter'
], function(AnnotationUtils, PlaceUtils, AbstractHighlighter) {

  var Highlighter = function(grid) {

    var annotationIndex = {}, // To keep track of annotationId -> rowIdx concordances

        dataView = grid.getData(),

        findById = function(id) {
          var rowIdx = annotationIndex[id];
          if (rowIdx)
            return dataView.getItem(rowIdx).__annotation;
        },

        isEmpty = function() {
          return jQuery.isEmptyObject(annotationIndex);
        },

        bindAnnotation = function(annotation) {
          var rowIdx = parseInt(annotation.anchor.substring(4)),
              row = dataView.getItem(rowIdx);

          row.__annotation = annotation;

          if (annotation.annotation_id)
            annotationIndex[annotation.annotation_id] = rowIdx;

          dataView.updateItem(row.id, row);
        },

        initPage = function(annotations) {
          dataView.beginUpdate();
          annotations.forEach(bindAnnotation);
          dataView.endUpdate();
        },

        removeAnnotation = function(annotation) {
          var annotationId = annotation.annotation_id,
              rowIdx = annotationIndex[annotationId],
              row;

          if (rowIdx) {
            row = dataView.getItem(rowIdx);

            delete annotationIndex[annotationId];
            delete row.__annotation;
            
            dataView.updateItem(row.id, row);
          }
        },

        convertSelectionToAnnotation = function(selection) {
          bindAnnotation(selection.annotation);
        };

    this.findById = findById;
    this.isEmpty = isEmpty;
    this.initPage = initPage;
    this.refreshAnnotation = bindAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;

    AbstractHighlighter.apply(this);
  };
  Highlighter.prototype = Object.create(AbstractHighlighter.prototype);

  /** SlickGrid Formatter for the 'annotation' cell **/
  Highlighter.CellFormatter = function(row, cell, val, columnDef, dataContext) {
    if (dataContext.__annotation) {
      var annotation = dataContext.__annotation,

          entityBody = AnnotationUtils.getFirstEntity(annotation),

          statusValues = AnnotationUtils.getStatus(annotation),

          label = (function() {
            var uri, label;
            if (entityBody && entityBody.uri) {
              uri = PlaceUtils.parseURI(entityBody.uri);
              label = uri.shortcode + ':' + uri.id;
              return '<span class="shortcode">' + label + '</span>';
            } else {
              return '&nbsp;';
            }
          })(),

          entityType = (entityBody) ? entityBody.type : false,

          cssClass = (entityType) ?
            'annotation ' + entityType.toLowerCase() + ' ' + statusValues.join(' ') :
            'annotation comment';

      return '<span class="' + cssClass + '" title="' + entityType + '">' + label + '</span>';
    }
  };

  return Highlighter;

});
