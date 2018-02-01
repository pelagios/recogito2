define([
  'document/map/style/palette'
], function(Palette) {

  var SLIDE_DURATION = 100;

  var Legend = function(parentEl, toggleButton) {

        // To work anround the otherwise circular dependency
    var MapStyle = require('document/map/style/mapStyle'),

        element = jQuery(
          '<div class="map-legend">' +
            '<div class="map-legend-head"></div>' +
            '<div class="map-legend-body">' +
              '<ul class="values">' +
              '</ul>' +
              '<ul class="non-distinct">' +
                '<li><span class="key multi" /><span class="label">Multiple values</span></li>' +
                '<li><span class="key no-val" /><span class="label">No value</span></li>' +
              '</ul>' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        legend = element.find('ul.values'),

        init = function() {
          toggleButton.click(toggle);
          element.find('.key.multi').css('backgroundColor', '#fff');
          element.find('.key.no-val').css({
            borderColor: MapStyle.POINT_DISABLED.color,
            backgroundColor: MapStyle.POINT_DISABLED.fillColor
          });
        },

        setLegend = function(values) {
          legend.empty();
          jQuery.each(values, function(name, color) {
            var row = jQuery(
              '<li><span class="key"></span><span class="label">' + name + '</span></li>');

            row.find('.key').css({
              backgroundColor: color,
              borderColor: Palette.darker(color)
            });

            legend.append(row);
          });
        },

        open = function() {
          element.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        close = function() {
          element.velocity('slideUp', { duration: SLIDE_DURATION });
        },

        toggle = function() {
          if (element.is(':visible')) close();
          else open();
        };

    init();

    this.close = close;
    this.open = open;
    this.setLegend = setLegend;
  };

  return Legend;

});
