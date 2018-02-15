define([
  'common/utils/annotationUtils',
  'document/annotation/text/selection/style/palette'
], function(AnnotationUtils, Palette) {

  var LEGEND = {},

      newColor = function(tag) {
        var registeredTags = Object.keys(LEGEND),
            idx = registeredTags.length % Palette.CATEGORY_9.length,
            color = Palette.CATEGORY_9[idx];

        LEGEND[tag] = color;
        return color;
      };

  return {

    getStyle: function(annotation) {
      var tags = AnnotationUtils.getTags(annotation),
          first = (tags.length > 0) ? tags[0] : false, // TODO temporary - color by first tag only
          storedColor, color;

      if (first) {
        storedColor = LEGEND[first];
        color = (storedColor) ? storedColor : newColor(first);
        return { 'title': tags.join(', '), 'color': color };
      }
    }

  };

});
