define([
  'common/utils/annotationUtils',
  'document/map/style/palette',
], function(AnnotationUtils, Palette) {

  var ByTagRule = function(annotationView) {

        // To work anround the otherwise circular dependency
    var MapStyle = require('document/map/style/mapStyle'),

        self = this,

        /** tag -> color lookup table **/
        legend = {},

        initLegend = function() {
          var tags = annotationView.listUniqueTags(),
              colors = (tags.length > 10) ? Palette.CATEGORY_17 : Palette.CATEGORY_10,
              numColors = colors.length;

          tags.forEach(function(tag, idx) {
            var colIdx = idx % numColors;
            legend[tag] = colors[colIdx];
          });
        },

        getLegend = function() {
          return legend;
        },

        getTags = function(annotations) {
          var asSet = annotations.reduce(function(set, annotation) {
                if (set.size < 2) {
                  var tags = AnnotationUtils.getTags(annotation);
                  tags.forEach(function(t) { set.add(t); });
                }

                return set;
              }, new Set());

          return Array.from(asSet);
        },

        // TODO remove redundancy with getShapeStyle
        getPointStyle = function(place, annotations) {
          var tags = getTags(annotations);
          if (tags.length == 1)
            return MapStyle.pointStyle(legend[tags[0]]);
          else if (tags.length > 1)
            return MapStyle.POINT_MULTI;
          else
            return MapStyle.POINT_DISABLED;
        },

        getShapeStyle = function(place, annotations) {
          var tags = getTags(annotations);
          if (tags.length == 1)
            return MapStyle.shapeStyle(legend[tags[0]]);
          else if (tags.length > 1)
            return MapStyle.SHAPE_MULTI;
          else
            return MapStyle.SHAPE_DISABLED;
        };

    initLegend();

    this.getLegend = getLegend;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;
  };

  return ByTagRule;

});
