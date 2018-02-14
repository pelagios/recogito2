define([
  'common/ui/palette',
  'common/utils/annotationUtils'
], function(Palette, AnnotationUtils) {

  return {

    getColor: function(annotation) {
      var tags = AnnotationUtils.getTags(annotation);

      // return 'red';
    }

  };

});
