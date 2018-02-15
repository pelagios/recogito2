define([
  'common/hasEvents',
  'document/map/style/rules/baseRule',
  'document/map/style/palette'
], function(HasEvents, Rules, Palette) {

  var SLIDE_DURATION = 100;

  var Legend = function(parentEl, toggleButton) {

    var self = this,

        btnLabel = {
          closed: 'Change colour and filter settings',
          open: 'Hide colour and filter settings'
        },

        element = jQuery(
          '<div class="map-legend">' +
            '<div class="style-selection">' +
              '<span>Color by<span>' +
              '<select>' +
                '<option selected="true" value>-</option>' +
                '<option value="BY_TAG">Tag</option>' +
                '<option value="BY_PART">Part</option>' +
                '<option value="BY_STATUS">Status</option>' +
                '<option value="BY_CONTRIBUTOR">Contributor</option>' +
                '<option value="BY_ACTIVITY" disabled="true">Latest activity</option>' +
                '<option value="BY_FIRST_OCCURRENCE" disabled="true">First occurrence</option>' +
                '<option value="BY_LAST_OCCURRENCE" disabled="true">Last occurrence</option>' +
              '</select>' +
            '</div>' +
            '<div class="map-legend-body">' +
              '<ul class="values"></ul>' +
              '<ul class="non-distinct">' +
                '<li><span class="key multi" /><span class="label">Multiple values</span></li>' +
                '<li><span class="key no-val" /><span class="label">No value</span></li>' +
              '</ul>' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        legend = element.find('ul.values'),

        selection = element.find('select'),

        buttonLabel = toggleButton.find('.label'),

        nonDistinct = element.find('.non-distinct').hide(),

        init = function() {
          buttonLabel.html(btnLabel.closed);
          toggleButton.click(toggle);

          selection.change(function(e) {
            var val = (this.value) ? this.value : undefined;
            self.fireEvent('changeStyle', val);
          });

          element.find('.key.multi').css('backgroundColor', '#fff');
          element.find('.key.no-val').css({
            borderColor: Rules.POINT_DISABLED.color,
            backgroundColor: Rules.POINT_DISABLED.fillColor
          });
        },

        update = function(settings) {
          legend.empty();
          jQuery.each(settings.legend, function(name, color) {
            var label = (settings.formatter) ? settings.formatter(name) : name,
                row = jQuery(
                  '<li><span class="key"></span><span class="label">' + label + '</span></li>');

            row.find('.key').css({
              backgroundColor: color,
              borderColor: Palette.darker(color)
            });

            legend.append(row);
          });

          if (settings.hasNonDistinct) nonDistinct.show(); else nonDistinct.hide();
        },

        clear = function() {
          legend.empty();
          nonDistinct.hide();
        },

        open = function() {
          element.velocity('slideDown', { duration: SLIDE_DURATION });
          buttonLabel.html(btnLabel.open);
        },

        close = function() {
          element.velocity('slideUp', { duration: SLIDE_DURATION });
          buttonLabel.html(btnLabel.closed);
        },

        toggle = function() {
          if (element.is(':visible')) close();
          else open();
        };

    init();

    this.clear = clear;
    this.close = close;
    this.open = open;
    this.update = update;

    HasEvents.apply(this);
  };
  Legend.prototype = Object.create(HasEvents.prototype);

  return Legend;

});
