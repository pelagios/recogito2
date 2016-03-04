define(['../common/annotationUtils'], function(AnnotationUtils) {

  var AnnotationRenderer = function(rootNode, annotations, highlighter) {

    var contentNode = rootNode.childNodes[0],

        sortByOffsetDesc= function(annotations) {
          return annotations.sort(function(a, b) {
            var offsetA = a.anchor.substr(12),
                offsetB = b.anchor.substr(12);

            return offsetB - offsetA;
          });
        };

    jQuery.map(sortByOffsetDesc(annotations), function(annotation) {
      var anchor = annotation.anchor.substr(12),
          quote = AnnotationUtils.getQuote(annotation),
          entityType = AnnotationUtils.getEntityType(annotation),
          range = rangy.createRange();

      range.selectCharacters(contentNode, parseInt(anchor), parseInt(anchor) + quote.length);
      highlighter.wrapRange(range, 'entity ' + entityType, rootNode);
    });
  };

  return AnnotationRenderer;

});
