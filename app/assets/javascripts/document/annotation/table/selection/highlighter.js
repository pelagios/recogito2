define([
  'document/annotation/common/selection/abstractHighlighter'
], function(AbstractHighlighter) {

  var Highlighter = function() {

    var findById = function(id) {

        },

        initPage = function(annotations) {
          console.log(annotations);
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

  return Highlighter;

});
