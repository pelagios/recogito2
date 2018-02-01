define([
  'common/hasEvents',
  'document/map/style/palette'
], function(HasEvents, Palette) {

  var SLIDE_DURATION = 100;

  var Legend = function(parentEl, toggleButton) {

    var self = this,

        // To work anround the otherwise circular dependency
        MapStyle = require('document/map/style/mapStyle'),

        element = jQuery(
          '<div class="map-legend">' +
            '<div class="map-legend-head"></div>' +
            '<div class="map-legend-body">' +
              '<div class="style-selection">' +
                '<span>Color by<span>' +
                '<select>' +
                  '<option selected="true" value>-</option>' +
                  '<option value="BY_PART" disabled="true">Document part</option>' +
                  '<option value="BY_STATUS">Verification Status</option>' +
                  '<option value="BY_TAG">Tag</option>' +
                  '<option value="BY_USER" disabled="true">User</option>' +
                '</select>' +
              '</div>' +
              '<ul class="values">' +
              '</ul>' +
              '<ul class="non-distinct">' +
                '<li><span class="key multi" /><span class="label">Multiple values</span></li>' +
                '<li><span class="key no-val" /><span class="label">No value</span></li>' +
              '</ul>' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        legend = element.find('ul.values'),

        selection = element.find('select'),

        init = function() {
          toggleButton.click(toggle);

          selection.change(function(e) {
            var val = (this.value) ? this.value : undefined;
            self.fireEvent('changeStyle', val);
          });

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

        clear = function() {
          legend.empty();
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

    this.clear = clear;
    this.close = close;
    this.open = open;
    this.setLegend = setLegend;

    HasEvents.apply(this);
  };
  Legend.prototype = Object.create(HasEvents.prototype);

  return Legend;

});
