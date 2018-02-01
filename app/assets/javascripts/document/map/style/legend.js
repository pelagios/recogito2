define([
  'document/map/style/palette'
], function(Palette) {

  var SLIDE_DURATION = 100;

  var Legend = function(parentEl, toggleButton) {

    var element = jQuery(
          '<div class="map-legend">' +
            '<div class="map-legend-head"></div>' +
            '<div class="map-legend-body">' +
              '<ul class="values">' +
              '</ul>' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        legend = element.find('ul.values'),

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

    toggleButton.click(toggle);

    this.close = close;
    this.open = open;
    this.setLegend = setLegend;
  };

  return Legend;

});
