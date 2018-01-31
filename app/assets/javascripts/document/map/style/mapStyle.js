define([
  'common/hasEvents',
  'document/map/style/rules/byTagRule',
  'document/map/style/palette'
], function(HasEvents, ByTag, Palette) {

  var MapStyle = function() {

    var self = this,

        annotations = false,

        currentRule = false,

        init = function(annotationView) {
          annotations = annotationView;
        },

        change = function(name) {

        },

        getPointStyle = function(place, annotations) {
          if (currentRule)
            return currentRule.getPointStyle(place, annotations);
          else
            return MapStyle.DEFAULT_POINT_STYLE;
        },

        getShapeStyle = function(place, annotations) {
          if (currentRule)
            return currentRule.getShapeStyle(place, annotations);
          else
            return MapStyle.DEFAULT_SHAPE_STYLE;
        };

    this.change = change;
    this.init = init;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;

    HasEvents.apply(this);
  };
  MapStyle.prototype = Object.create(HasEvents.prototype);

  MapStyle.DEFAULT_POINT_STYLE = {
    color       : Palette.DEFAULT_STROKE_COLOR,
    fillColor   : Palette.DEFAULT_FILL_COLOR,
    opacity     : 1,
    weight      : 1.5,
    fillOpacity : 1
  };

  MapStyle.DEFAULT_SHAPE_STYLE = {
    color       : Palette.DEFAULT_STROKE_COLOR,
    fillColor   : Palette.DEFAULT_FILL_COLOR,
    opacity     : 1,
    weight      : 1.5,
    fillOpacity : 0.8
  };

  MapStyle.pointStyle = function(color) {
    return jQuery.extend({}, MapStyle.DEFAULT_POINT_STYLE, {
      color: Palette.darker(color),
      fillColor: color
    });
  };

  MapStyle.shapeStyle = function(color) {
    return jQuery.extend({}, MapStyle.DEFAULT_SHAPE_STYLE, {
      color: Palette.darker(color),
      fillColor: color
    });
  };

  return MapStyle;

});
