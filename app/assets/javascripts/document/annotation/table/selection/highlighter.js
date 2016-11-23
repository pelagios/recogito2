define([
  'common/utils/annotationUtils',
  'document/annotation/common/selection/abstractHighlighter'
], function(AnnotationUtils, AbstractHighlighter) {

  var Highlighter = function(grid) {

    var annotationIndex = {}, // To keep track of annotationId -> rowIdx concordances

        dataView = grid.getData(),

        findById = function(id) {
          var rowIdx = annotationIndex[id];
          if (rowIdx)
            return dataView.getItem(rowIdx).__annotation;
        },

        bindAnnotation = function(annotation) {
          var rowIdx = parseInt(annotation.anchor.substring(4)),
              row = dataView.getItem(rowIdx);

          row.__annotation = annotation;
          annotationIndex[annotation.annotation_id] = rowIdx;
          dataView.updateItem(rowIdx, row);
        },

        initPage = function(annotations) {
          jQuery.each(annotations, function(idx, annotation) {
            bindAnnotation(annotation);
          });
        },

        removeAnnotation = function(annotation) {
          var annotationId = annotation.annotation_id,
              rowIdx = annotationIndex[annotationId],
              row;

          if (rowIdx) {
            row = dataView.getItem(rowIdx);

            delete annotationIndex[annotationId];
            delete row.__annotation;
            dataView.updateItem(rowIdx, row);
          }
        },

        convertSelectionToAnnotation = function(selection) {
          bindAnnotation(selection.annotation);
        };

    this.findById = findById;
    this.initPage = initPage;
    this.refreshAnnotation = function() {}; // Not needed for tables
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;

    AbstractHighlighter.apply(this);
  };
  Highlighter.prototype = Object.create(AbstractHighlighter.prototype);

  /** Formatter for the 'annotation' indicator cell **/
  Highlighter.CellFormatter = function(row, cell, val, columnDef, dataContext) {
    if (dataContext.__annotation) {
      var annotation = dataContext.__annotation,
          entityType = AnnotationUtils.getEntityType(annotation),
          cssClass = (entityType) ? 'annotation ' + entityType.toLowerCase() : 'annotation';
          label = (entityType) ? entityType : '&nbsp;';

      return '<span class="' + cssClass + '">' + label + '</span>';
    }
  };

  return Highlighter;

});
