define([], function() {

  var Timefilter = function(containerEl) {

    var element = jQuery(
          '<div class="timefilter-popup">' +
            '<span class="label">Apr 21</span>' +
            '<input type="range" step="86400" />' +
            '<span class="label">Aug 5</span>' +
            '<div class="current"></div>' +
          '</div>').hide(),

        slider = element.find('input'),

        currentValue = element.find('.current'),

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else 
            element.show();
        },

        onInput = function(evt) {
          var currentDate = new Date(evt.target.value * 1000);
          currentValue.html(currentDate.toLocaleDateString('en-US', {
            day: 'numeric', month: 'short'
          }));
        },

        onChange = function(evt) {
          console.log('change!');
        },

        setBounds = function(fromDate, toDate) {
          slider.attr('min', new Date('2019-04-21').getTime() / 1000);
          slider.attr('max', new Date('2019-08-05').getTime() / 1000);
        };

    slider.on('input', onInput);
    slider.on('change', onChange);

    // For testing only
    setBounds();
    
    jQuery(containerEl).append(element);

    this.toggle = toggle;
    this.setBounds = setBounds;
  };

  return Timefilter;

});