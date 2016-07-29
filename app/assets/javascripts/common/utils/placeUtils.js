define([], function() {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_GAZETTEERS = [
        { shortcode: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ], color: '#1f77b4' },
        { shortcode: 'dare', url_patterns: [ 'http://dare.ht.lu.se/places/' ], color: '#ff7f0e' },
        { shortcode: 'geonames', url_patterns: ['http://sws.geonames.org/'], color: '#2ca02c' }
      ],

      /**
       * Maps the list of conflated records to a list values of the given
       * record property. (E.g. go from list of records to list of descriptions.)
       */
      mapConflated = function(place, key) {
        var mapped = [];
        jQuery.each(place.is_conflation_of, function(idx, record) {
          var values = record[key];
          if (values)
            if (jQuery.isArray(values))
              mapped = mapped.concat(values);
            else
              mapped.push(values);
        });

        return mapped;
      };

  return {

    /**
     * Given a placename, returns the 'best matching record'.
     *
     * The method will simply look for an exact match in all names of all records
     * conflated in this place, and return the first matching record. If no record
     * is found, the method will just return the first record in the list.
     */
    getBestMatchingRecord: function(place, name) {
      var bestMatch = place.is_conflation_of[0];
      jQuery.each(place.is_conflation_of, function(idx, record) {
        var names = jQuery.map(record.names, function(obj) { return obj.name; });
        if (names.indexOf(name) > -1) {
          bestMatch = record;
          return false; // Match found - break loop
        }
      });
      return bestMatch;
    },

    /** Returns the descriptions from all records conflated in this place **/
    getDescriptions : function(place) { return mapConflated(place, 'descriptions'); },

    /**
     * Returns a list of distinct names (without language) contained in this record,
     * sorted by frequency. The method also splits on separator characters (comma and /),
     * so the results can be used as screen labels.
     *
     * If skipTitles is set to true, the labels will *not* inlude names appearing in the title.
     * This is to simplify the typical use case where title is shown in the UI, and all
     * alternative names underneath (where repetition of title is not desired).
     */
    getLabels: function(gazetteerRecord, skipTitles) {
      var titles = (skipTitles) ? gazetteerRecord.title.split(/,|\//) : false,
          labels = {}, asArray = [], result,

          add = function(name) {
            jQuery.each(name.split(/,|\//), function(idx, label) {
              var trimmed = label.trim(),
                  count = labels[trimmed];

              if (count)
                labels[trimmed] = count + 1;
              else
                labels[trimmed] = 1;
            });
          },

          // Computes the diff between two string arrays. Result is array A, minus string
          // appearing in array B. Strings are trimmed for comparison.
          diff = function(arrayA, arrayB) {
            var trimmedB = jQuery.map(arrayB, function(elem) { return elem.trim(); });
            return jQuery.grep(arrayA, function(elem) {
              return trimmedB.indexOf(elem.trim()) < 0;
            });
          };

      // Collect title and all names, along with their frequency
      add(gazetteerRecord.title);
      jQuery.each(gazetteerRecord.names, function(idx, n) {
        add(n.name);
      });

      // Sort by frequency
      jQuery.each(labels, function(label, count) {
        asArray.push([label, count]);
      });

      asArray.sort(function(a, b) {
        return b[1] - a[1];
      });

      result = jQuery.map(asArray, function(val) {
        return val[0];
      });

      if (skipTitles)
        return diff(result, titles);
      else
        return result;
    },

    /** Returns the record with the given URI (or false, if none) **/
    getRecord: function(place, uri) {
      var bestMatch = false;
      jQuery.each(place.is_conflation_of, function(idx, record) {
        if (record.uri === uri) {
          bestMatch = record;
          return false; // Match found - break loop
        }
      });
      return bestMatch;
    },

    /** Returns the URIs of all records conflated in this place **/
    getURIs : function(place) { return mapConflated(place, 'uri'); },

    /**
     * Parses a gazetteer URI and determines the appropriate gazetteer
     * shortcode, ID, and signature color.
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

});
