define(['../common/annotationUtils'], function(AnnotationUtils) {

  var AnnotationRenderer = function(rootNode, annotations) {

    var contentNode = rootNode.childNodes[0],

        sortByOffsetDesc= function(annotations) {
          return annotations.sort(function(a, b) {
            var offsetA = a.anchor.substr(12),
                offsetB = b.anchor.substr(12);

            return offsetB - offsetA;
          });
        },

        ranges = jQuery.map(sortByOffsetDesc(annotations), function(annotation) {
          var anchor = annotation.anchor.substr(12),
          quote = AnnotationUtils.getQuote(annotation),
          entityType = AnnotationUtils.getEntityType(annotation),
          range = rangy.createRange();

          range.setStart(contentNode, parseInt(anchor));
          range.setEnd(contentNode, parseInt(anchor) + quote.length);

          // TODO find a better solution!
          range.entityType = entityType;

          return range;
        });

    jQuery.each(ranges, function(idx, range) {
      var wrapper = document.createElement('SPAN');
      wrapper.className = 'entity ' + range.entityType;
      range.surroundContents(wrapper);
    });
  };

  return AnnotationRenderer;

});
