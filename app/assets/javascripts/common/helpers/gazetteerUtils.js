define(function() {

  // TODO need to put this on the server, where we can feed it from the DB
  var KNOWN_GAZETTEERS = [
    { shortcode: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ], color: '#1f77b4' },
    { shortcode: 'dare', url_patterns: [ 'http://dare.ht.lu.se/places/' ], color: '#ff7f0e' }
  ];

  var GazetteerUtils = {

    /**
     * Parses a gazetteer URI and determines the appropriate gazetteer shortcode,
     * ID, and signature color.
     */
    parseURI : function(uri) {
      var parseResult = { uri: uri };

      jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
        var cont = true;
        jQuery.each(g.url_patterns, function(j, pattern) {
          if (uri.indexOf(pattern) === 0) {
            parseResult.shortcode = g.shortcode;
            parseResult.id = uri.substring(pattern.length);
            parseResult.color = g.color;

            cont = false;
            return cont;
          }
        });
        return cont;
      });

      return parseResult;
    }

  };

  return GazetteerUtils;

});
