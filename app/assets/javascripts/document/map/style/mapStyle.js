define([
  'common/hasEvents',
  'document/map/style/palette'
], function(HasEvents, Palette) {

  var BASE_POINT_STYLE = {
        color       : Palette.DEFAULT_STROKE_COLOR,
        fillColor   : Palette.DEFAULT_FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 1
      },

      BASE_SHAPE_STYLE = {
        color       : Palette.DEFAULT_STROKE_COLOR,
        fillColor   : Palette.DEFAULT_FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 0.5
      };

  var MapStyle = function() {

    var self = this,

        change = function(name) {

        },

        getPointStyle = function(place) {
          return BASE_POINT_STYLE;
        },

        getShapeStyle = function(place) {
          return BASE_SHAPE_STYLE;
        };

    this.change = change;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;

    HasEvents.apply(this);
  };
  MapStyle.prototype = Object.create(HasEvents.prototype);

  return MapStyle;

});
