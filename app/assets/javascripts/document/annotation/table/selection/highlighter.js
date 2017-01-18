define([
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'document/annotation/common/selection/abstractHighlighter'
], function(AnnotationUtils, PlaceUtils, AbstractHighlighter) {

  var Highlighter = function(grid) {

    var dataView = grid.getData(),

        // To keep track of annotationId -> rowId concordances
        annotationIndex = {},

        // To keep track of row offset -> rowId concordances
        rowIndex = {},

        findById = function(annotationId) {
          var rowId = annotationIndex[annotationId];
          if (rowId)
            return dataView.getItemById(rowId).__annotation;
        },

        isEmpty = function() {
          return jQuery.isEmptyObject(annotationIndex);
        },

        bindAnnotation = function(annotation) {
          var rowOffset = parseInt(annotation.anchor.substring(4)),
              rowId = rowIndex[rowOffset],
              row = dataView.getItemById(rowId);

          row.__annotation = annotation;

          if (annotation.annotation_id)
            annotationIndex[annotation.annotation_id] = rowId;

          dataView.updateItem(rowId, row);
        },

        initPage = function(annotations) {
          var initRowIndex = function() {
                dataView.getItems().forEach(function(row, idx) {
                  rowIndex[idx] = row.id;
                });
              };

          initRowIndex();

          dataView.beginUpdate();
          annotations.forEach(bindAnnotation);
          dataView.endUpdate();
        },

        removeAnnotation = function(annotation) {
          var annotationId = annotation.annotation_id,
              rowId = annotationIndex[annotationId],
              row;

          if (rowId) {
            row = dataView.getItemById(rowId);

            delete annotationIndex[annotationId];
            delete row.__annotation;

            dataView.updateItem(rowId, row);
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
