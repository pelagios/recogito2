define([], function() {

  var Timefilter = function(containerEl) {

    var element = jQuery(
          '<div class="timefilter-popup">' +
            '<span class="label">Apr 21</span>' +
            '<input type="range" />' +
            '<span class="label">Aug 5</span>' +
          '</div>').hide(),

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else 
            element.show();
        },

        setBounds = function(fromDate, toDate) {

        };

    jQuery(containerEl).append(element);

    this.toggle = toggle;
  };

  return Timefilter;

});