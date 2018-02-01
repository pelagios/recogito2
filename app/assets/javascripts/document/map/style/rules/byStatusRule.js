define([
  'common/utils/annotationUtils',
  'document/map/style/palette'
], function(AnnotationUtils, Palette) {

  var ByStatusRule = function(annotationView) {

        // To work anround the otherwise circular dependency
    var MapStyle = require('document/map/style/mapStyle'),

        legend =  {},

        initLegend = function(values) {
          var colors = (values.length > 10) ? Palette.CATEGORY_17 : Palette.CATEGORY_10,
              numColors = colors.length;

          values.forEach(function(tag, idx) {
            var colIdx = idx % numColors;
            legend[tag] = colors[colIdx];
          });
        },

        getLegend = function() {
          return legend;
        },

        getValues = function(annotations) {
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
        },

        getPointStyle = function(place, annotations) {
          var values = getValues(annotations);
          if (values.length == 1)
            return MapStyle.pointStyle(legend[values[0]]);
          else if (values.length > 1)
            return MapStyle.POINT_MULTI;
          else
            return MapStyle.POINT_DISABLED;
        },

        getShapeStyle = function(place, annotations) {
          var values = getValues(annotations);
          if (values.length == 1)
            return MapStyle.shapeStyle(legend[values[0]]);
          else if (values.length > 1)
            return MapStyle.SHAPE_MULTI;
          else
            return MapStyle.SHAPE_DISABLED;
        };

    initLegend(['UNVERIFIED', 'VERIFIED']);

    this.getLegend = getLegend;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;
  };

  return ByStatusRule;

});
