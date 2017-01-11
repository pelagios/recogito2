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
          dataView.updateItem(row.id, row);
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
    this.refreshAnnotation = bindAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;

    AbstractHighlighter.apply(this);
  };
  Highlighter.prototype = Object.create(AbstractHighlighter.prototype);

  /** SlickGrid Formatter for the 'annotation' cell **/
  Highlighter.CellFormatter = function(row, cell, val, columnDef, dataContext) {
    var ICONS = {
          'PLACE'  : '&#xf041;',
          'PERSON' : '&#xf007;',
          'EVENT'  : '&#xf005;'
        },

        getIcon = function(entityType) {
          var icon = ICONS[entityType];
          if (icon) return icon; else return '&#xf02b;';
        };

    if (dataContext.__annotation) {
      var annotation = dataContext.__annotation,
          entityType = AnnotationUtils.getEntityType(annotation),
          cssClass = (entityType) ? 'annotation ' + entityType.toLowerCase() : 'annotation';

      return '<span class="' + cssClass + '" title="' + entityType + '">' + getIcon(entityType) + '</span>';
    }
  };

  return Highlighter;

});
