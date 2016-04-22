define(function() {

  // TODO we should put this on the server, where we can feed it from the DB
  var KNOWN_GAZETTEERS = [
    { prefix: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ], colors: [ '#1f77b4', '#1b699e'] },
    { prefix: 'dare', url_patterns: [ 'http://dare.ht.lu.se/places/' ], colors: [ '#f58929', '#cd660a' ] }
  ];

  return {

    /** Shortens a gazetteer URI to a shortcode **/
    formatGazetteerURI : function(uri) {
      var shortCode = uri;
          // style = 'background-color:#ddd; border:1px solid #ccc;';

      jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
        var cont = true;
        jQuery.each(g.url_patterns, function(j, pattern) {
          if (uri.indexOf(pattern) === 0) {
            shortCode = g.prefix + ':' + uri.substring(pattern.length);
            // style = 'background-color:' + g.colors[0] + '; border:1px solid ' + g.colors[1] + ';';
            cont = false;
            return cont;
          }
        });
        return cont;
      });

      return '<a class="gazetteer-id" href="' + uri + '" target="_blank">' + shortCode + '</a>';
    },

    /** Formats a yyyyMMddToYear to YYYY [Era] **/
    yyyyMMddToYear : function(str) {
      var era = (str.indexOf('-') === 0) ? ' BC' : '',
          year = (str.indexOf('-') === 0) ?
            str.substring(1, str.indexOf('-', 1)) :
            str.substring(0, str.indexOf('-'));

      return parseInt(year) + era;
    }

  };

});
