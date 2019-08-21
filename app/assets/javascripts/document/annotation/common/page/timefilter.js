define(['common/hasEvents'], function(HasEvents) {

  var formatDate = function(date) {
    return date.toLocaleDateString('en-US', { day: 'numeric', month: 'short' });
  };

  var Timefilter = function(containerEl, fromTimestamp, toTimestamp) {

    var self = this,

        element = jQuery(
          '<div class="timefilter-popup">' +
            '<div class="hint">Hide annotations older than</div>' +
            '<div class="slider-container">' +
              '<span class="label from"></span>' +
              '<input type="range" min="' + 
                (fromTimestamp / 1000) + '" max="' + (toTimestamp / 1000) + '" step="86400" value="' + 
                (fromTimestamp/ 1000) + '" />' +
              '<span class="label to"></span>' +
            '</div>' +
            '<div class="current"></div>' +
          '</div>').hide(),

        labelFrom = element.find('.label.from'),

        labelTo = element.find('.label.to'),

        currentValue = element.find('.current'),

        slider = element.find('input'),

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else 
            element.show();
        },

        onInput = function(evt) {
          var currentDate = new Date(evt.target.value * 1000);
          currentValue.html(formatDate(currentDate));
        },

        onChange = function(evt) {
          self.fireEvent('change', new Date(evt.target.value * 1000));
        },

        init = function() {
          var fromStr = formatDate(new Date(fromTimestamp)),
              toStr = formatDate(new Date(toTimestamp));

          labelFrom.html(fromStr);
          labelTo.html(toStr);

          currentValue.html(fromStr);

          slider.on('input', onInput);
          slider.on('change', onChange);
          
          jQuery(containerEl).append(element);      
        }

    init();

    this.toggle = toggle;

    HasEvents.apply(this);
  };
  Timefilter.prototype = Object.create(HasEvents.prototype);

  return Timefilter;

});