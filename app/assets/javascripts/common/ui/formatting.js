define(function() {

  var MONTH_NAMES_SHORT = [
    'Jan', 'Feb', 'Mar', 'Apr',
    'May', 'Jun', 'Jul', 'Aug',
    'Sept', 'Oct', 'Nov', 'Dec' ];

  return {

    /** Formats a yyyyMMddToYear to YYYY [Era] **/
    yyyyMMddToYear: function(str) {
      var era = (str.indexOf('-') === 0) ? ' BC' : '',
          year = (str.indexOf('-') === 0) ?
            str.substring(1, str.indexOf('-', 1)) :
            str.substring(0, str.indexOf('-'));

      return parseInt(year) + era;
    },

    /** Formats absolute time into a human-readable 'relative' label (e.g. '2 minutes ago') **/
    timeSince: function(date) {
      if (date)
        return jQuery.timeago(date);
      else
        return '';
    },

    /** Current implementation relies on numeral.js **/
    formatNumber : function(n) {
      return numeral(n).format('0,0');
    },

    formatDay : function(date) {
      var day = date.getDate(),
          month = date.getMonth(),
          year = date.getFullYear();

      return MONTH_NAMES_SHORT[month] + ' ' + day + ', ' + year;
    },

    initTextDirection : function(containerEl) {
      var plaintext = containerEl.innerHTML.substring(0, 200),

          isRTL = function() {
            var ltrChars = 0,
                rtlChars = 0,
                charCode;

            for (var i = 0, len = plaintext.length; i < len; i++) {
              charCode = plaintext[i].charCodeAt();
              if (charCode > 64) {
                if (charCode < 1478)
                  ltrChars++;
                else
                  rtlChars++;
              }
            }

            return rtlChars > ltrChars;
          };

      if (isRTL())
        jQuery(containerEl).addClass('rtl');
    }

  };

});
