define([
  'document/map/style/rules/baseRule'
], function(BaseRule) {

  var ByStatusRule = function(annotationView) {

    var opts = {
          values: [ 'VERIFIED', 'UNVERIFIED' ],
          colorScales: [ [ '#31a354', '#7f7f7f' ] ]
        },

        getValues = function(place, annotations) {
          var asSet = annotations.reduce(function(set, annotation) {
                if (set.size < 2) {
                  var stati = annotation.bodies.reduce(function(result, body) {
                        if (body.status) result.push(body.status.value);
                        return result;
                      }, []);

                  stati.forEach(function(s) { set.add(s); });
                }

                return set;
              }, new Set());

          return Array.from(asSet);
        };

    BaseRule.apply(this, [ annotationView, getValues, opts ]);
  };
  ByStatusRule.prototype = Object.create(BaseRule.prototype);

  return ByStatusRule;

});
