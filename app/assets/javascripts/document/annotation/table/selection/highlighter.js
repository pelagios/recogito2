define([
  'common/utils/annotationUtils',
  'document/annotation/common/selection/abstractHighlighter'
], function(AnnotationUtils, AbstractHighlighter) {

  var Highlighter = function(grid) {

    var dataView = grid.getData(),

        findById = function(id) {

        },

        initPage = function(annotations) {
          jQuery.each(annotations, function(idx, annotation) {
            var rowIdx = parseInt(annotation.anchor.substring(4)),
                row = dataView.getItem(rowIdx);

            row.__annotation = annotation;
            dataView.updateItem(rowIdx, row);
          });
        },

        refreshAnnotation = function(annotation) {

        },

        removeAnnotation = function(annotation) {

        },

        convertSelectionToAnnotation = function(selection) {

        };

    this.findById = findById;
    this.initPage = initPage;
    this.refreshAnnotation = refreshAnnotation;
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
          label = (entityType) ? entityType : '';

      return '<span class="' + cssClass + '">' + label + '</span>';
    }
  };

  return Highlighter;

});
