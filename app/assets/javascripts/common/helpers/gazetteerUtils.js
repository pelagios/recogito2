define(function() {

  // TODO need to put this on the server, where we can feed it from the DB
  var KNOWN_GAZETTEERS = [
    { shortcode: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ] },
    { shortcode: 'dare', url_patterns: [ 'http://dare.ht.lu.se/places/' ] }
  ];

  var GazetteerUtils = {

    getShortCode : function(uri) {
      var shortCode = uri;

      jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
        var cont = true;
        jQuery.each(g.url_patterns, function(j, pattern) {
          if (uri.indexOf(pattern) === 0) {
            shortCode = g.prefix + ':' + uri.substring(pattern.length);
            cont = false;
            return cont;
          }
        });
        return cont;
      });
    },

    getID : function(uri) {

    },


  };

  return GazetteerUtils;

});
