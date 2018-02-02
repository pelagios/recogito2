define([
  'common/utils/annotationUtils',
  'document/map/style/rules/baseRule'
], function(AnnotationUtils, BaseRule) {

  var ByTagRule = function(annotationView) {

    var opts = {
          values: annotationView.listUniqueTags(),
          hasNonDistinct: true
        },

        getValues = function(place, annotations) {
          var asSet = annotations.reduce(function(set, annotation) {
                if (set.size < 2) {
                  var tags = AnnotationUtils.getTags(annotation);
                  tags.forEach(function(t) { set.add(t); });
                }

                return set;
              }, new Set());

          return Array.from(asSet);
        };

    BaseRule.apply(this, [ annotationView, getValues, opts ]);
  };
  ByTagRule.prototype = Object.create(BaseRule.prototype);

  return ByTagRule;

});
