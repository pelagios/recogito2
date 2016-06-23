define(function() {

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
      return jQuery.timeago(date);
    }

  };

});
