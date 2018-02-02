define([
  'document/map/style/palette'
], function(Palette) {

  var DEFAULT_POINT_STYLE = {
        color       : Palette.DEFAULT_STROKE_COLOR,
        fillColor   : Palette.DEFAULT_FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 1
      },

      DEFAULT_SHAPE_STYLE = {
        color       : Palette.DEFAULT_STROKE_COLOR,
        fillColor   : Palette.DEFAULT_FILL_COLOR,
        opacity     : 1,
        weight      : 1.5,
        fillOpacity : 0.8
      },

      POINT_DISABLED =
        jQuery.extend({}, DEFAULT_POINT_STYLE, {
          color: '#5e5e5e',
          fillColor: '#8f8f8f',
          weight:1.2
        }),

      POINT_MULTI =
        jQuery.extend({}, DEFAULT_POINT_STYLE, {
          color: '#000',
          fillColor: '#fff'
        }),

      SHAPE_DISABLED =
        jQuery.extend({}, DEFAULT_SHAPE_STYLE, {
          color: '#5e5e5e',
          opacity: 0.45,
          fillColor: '#8c8c8c'
        }),

      SHAPE_MULTI =
        jQuery.extend({}, DEFAULT_SHAPE_STYLE, {
          color: '#000',
          opacity: 0.6,
          fillColor: '#fff'
        }),

      pointStyle = function(color) {
        return jQuery.extend({}, DEFAULT_POINT_STYLE, {
          color: Palette.darker(color),
          fillColor: color
        });
      },

      shapeStyle = function(color) {
        return jQuery.extend({}, DEFAULT_SHAPE_STYLE, {
          color: Palette.darker(color),
          fillColor: color
        });
      };

  var BaseStyleRule = function(annotationView, getValues, opts) {

    var settings = jQuery.extend({}, opts, { legend: {} }),

        initLegend = function() {

          var choosePalette = function(numValues, palettes) {
                    // Sort available palettes by length, longest first
                var sorted = palettes.sort(function(a, b) { return b.length - a.length; }),
                    selected = sorted[0]; // Default to longest if none is long enough

                // Take the shortest that has enough colors
                sorted.forEach(function(palette) {
                  if (palette.length >= numValues)
                    selected = palette;
                });

                return selected;
              };

          var availableScales = opts.colorScales || [ Palette.CATEGORY_10, Palette.CATEGORY_17 ],
              colors = choosePalette(opts.values.length, availableScales),
              numColors = colors.length;

          opts.values.forEach(function(val, idx) {
            var colIdx = idx % numColors;
            settings.legend[val] = colors[colIdx];
          });
        },

        getSettings = function() {
          return settings;
        },

        getStyle = function(place, annotations, styles) {
          var values = getValues(place, annotations);
          if (values.length == 1)
            return styles.fn(settings.legend[values[0]]);
          else if (values.length > 1)
            return styles.multi;
          else
            return styles.disabled;
        },

        getPointStyle = function(place, annotations) {
          return getStyle(place, annotations, {
            fn      : pointStyle,
            multi   : POINT_MULTI,
            disabled: POINT_DISABLED
          });
        },

        getShapeStyle = function(place, annotations) {
          return getStyle(place, annotations, {
            fn      : shapeStyle,
            multi   : SHAPE_MULTI,
            disabled: SHAPE_DISABLED
          });
        };

    initLegend();

    this.getSettings = getSettings;
    this.getPointStyle = getPointStyle;
    this.getShapeStyle = getShapeStyle;
  };

  /** Make default styles visible to the outside as **/
  BaseStyleRule.DEFAULT_POINT_STYLE = DEFAULT_POINT_STYLE;
  BaseStyleRule.DEFAULT_SHAPE_STYLE = DEFAULT_SHAPE_STYLE;
  BaseStyleRule.POINT_DISABLED = POINT_DISABLED;
  BaseStyleRule.POINT_MULTI = POINT_MULTI;
  BaseStyleRule.POINT_DISABLED = SHAPE_DISABLED;
  BaseStyleRule.POINT_MULTI = SHAPE_MULTI;

  return BaseStyleRule;

});
