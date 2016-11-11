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

    setQueryParam : function(key, val) {
      var baseUrl = window.location.protocol + '//' + window.location.host + location.pathname,
          currentParams = this.parseQueryString();

      currentParams[key] = val;

      window.location.href = baseUrl + '?' + jQuery.param(currentParams);
    }

  };

});
