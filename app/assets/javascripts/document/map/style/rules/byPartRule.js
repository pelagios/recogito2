define([
  'common/config',
  'document/map/style/rules/baseRule'
], function(Config, BaseRule) {

  var ByContributorRule = function(annotationView) {

    var opts = {
          values: config.parts.map(function(p) { return p.id; }),

          formatter: function(partId) {
            var part = config.parts.find(function(p) {
              return p.id == partId;
            });
            if (part) return part.title;
          }
        },

        getValues = function(place, annotations) {
          var asSet = annotations.reduce(function(set, annotation) {
                if (set.size < 2)
                  set.add(annotation.annotates.filepart_id);
                return set;
              }, new Set());

          return Array.from(asSet);
        };

    BaseRule.apply(this, [ annotationView, getValues, opts ]);
  };
  ByContributorRule.prototype = Object.create(BaseRule.prototype);

  return ByContributorRule;

});
