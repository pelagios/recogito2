define([
  'document/map/style/rules/baseRule'
], function(BaseRule) {

  var ByContributorRule = function(annotationView) {

    var opts = {
          values: annotationView.listContributors(),
          hasNonDistinct: annotationView.listContributors().length > 1
        },

        getValues = function(place, annotations) {
          var asSet = annotations.reduce(function(set, annotation) {
                if (set.size < 2)
                  annotation.contributors.forEach(function(c) { set.add(c); });
                return set;
              }, new Set());

          return Array.from(asSet);
        };

    BaseRule.apply(this, [ annotationView, getValues, opts ]);
  };
  ByContributorRule.prototype = Object.create(BaseRule.prototype);

  return ByContributorRule;

});
