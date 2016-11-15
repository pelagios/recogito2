define([], function() {

  return {

    parseQueryString : function() {
      var qStr = location.search.substr(1),
          parsed =  {};

      if (qStr) {
        qStr.split('&').forEach(function(part) {
          var item = part.split('=');
          parsed[item[0]] = decodeURIComponent(item[1]);
        });
      }

      return parsed;
    },

    setQueryParams : function(params) {
      var baseUrl = window.location.protocol + '//' + window.location.host + location.pathname,
          currentParams = this.parseQueryString();

      jQuery.extend(currentParams, params);
      window.location.href = baseUrl + '?' + jQuery.param(currentParams);
    }

  };

});
