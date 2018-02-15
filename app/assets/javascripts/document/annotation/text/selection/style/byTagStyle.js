define([
  'common/utils/annotationUtils',
  'document/map/style/palette'
], function(AnnotationUtils, Palette) {

  var LEGEND = {},

      newColor = function(tag) {
        var registeredTags = Object.keys(LEGEND),
            idx = registeredTags.length % Palette.CATEGORY_17.length,
            color = Palette.CATEGORY_17[idx];

        LEGEND[tag] = color;
        return color;
      };

  return {

    getColor: function(annotation) {
      var tags = AnnotationUtils.getTags(annotation),

          // TODO hack
          first = (tags.length > 0) ? tags[0] : false;

      if (first) {
        var storedColor = LEGEND[first],
            color = (storedColor) ? storedColor : newColor(first);
        return color;
      }
    }

  };

});
