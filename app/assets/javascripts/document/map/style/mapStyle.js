define([
  'common/hasEvents',
  'document/map/style/rules/byTagRule',
  'document/map/style/legend',
  'document/map/style/palette'
], function(HasEvents, ByTagRule, Legend, Palette) {

  var MapStyle = function() {

    var self = this,

        legend = new Legend(jQuery('.map-container'), jQuery('.toggle-legend')),

        annotations = false,

        currentRule = false,

        init = function(annotationView) {
          annotations = annotationView;

          // TODO for testing only
          // currentRule = new ByTagRule(annotationView);
          // legend.setLegend(currentRule.getLegend());
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

  MapStyle.POINT_DISABLED =
    jQuery.extend({}, MapStyle.DEFAULT_POINT_STYLE, {
      color: '#5e5e5e',
      fillColor: '#8f8f8f',
      weight:1.2
    });

  MapStyle.POINT_MULTI =
    jQuery.extend({}, MapStyle.DEFAULT_POINT_STYLE, {
      color: '#000',
      fillColor: '#fff'
    });

  MapStyle.SHAPE_DISABLED =
    jQuery.extend({}, MapStyle.DEFAULT_SHAPE_STYLE, {
      color: '#5e5e5e',
      opacity: 0.45,
      fillColor: '#8c8c8c'
    });

  MapStyle.SHAPE_MULTI =
    jQuery.extend({}, MapStyle.DEFAULT_SHAPE_STYLE, {
      color: '#000',
      opacity: 0.6,
      fillColor: '#fff'
    });

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
