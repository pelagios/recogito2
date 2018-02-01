define([
  'common/hasEvents',
  'document/map/style/rules/baseRule',
  'document/map/style/rules/byStatusRule',
  'document/map/style/rules/byTagRule',
  'document/map/style/legend',
  'document/map/style/palette'
], function(HasEvents, Rules, ByStatusRule, ByTagRule, Legend, Palette) {

  var RULES = {
    BY_TAG: ByTagRule,
    BY_STATUS: ByStatusRule
  };

  var MapStyle = function() {

    var self = this,

        legend = new Legend(jQuery('.map-container'), jQuery('.toggle-legend')),

        annotations = false,

        currentRule = false,

        init = function(annotationView) {
          annotations = annotationView;
        },

        getPointStyle = function(place, annotations) {
          if (currentRule)
            return currentRule.getPointStyle(place, annotations);
          else
            return Rules.DEFAULT_POINT_STYLE;
        },

        getShapeStyle = function(place, annotations) {
          if (currentRule)
            return currentRule.getShapeStyle(place, annotations);
          else
            return Rules.DEFAULT_SHAPE_STYLE;
        },

        onChangeStyle = function(name) {
          var rule = RULES[name];
          if (rule) {
            currentRule = new rule(annotations);
            legend.setLegend(currentRule.getLegend());
          } else {
            currentRule = false;
            legend.clear();
          }

          self.fireEvent('change', name);
        };

    legend.on('changeStyle', onChangeStyle);

    this.init = init;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;

    HasEvents.apply(this);
  };
  MapStyle.prototype = Object.create(HasEvents.prototype);

  return MapStyle;

});
