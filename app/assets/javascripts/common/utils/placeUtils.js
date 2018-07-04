define([
  'common/ui/countries',
  'text!/api/authorities/gazetteers'
], function(Countries, Gazetteers) {

  // TODO fetch this information from the server, so we can feed it from the DB
  var TOKEN = '{{id}}',

      KNOWN_GAZETTEERS = JSON.parse(Gazetteers),

      /** Keeping previous hard-coded version for color/URL pattern reference **/
            
      // KNOWN_GAZETTEERS = [
      //   { shortcode: 'chgis',    url_patterns: [ 'http://maps.cga.harvard.edu/tgaz/placename/hvd_' ], color: '#9467bd' },
      //   { shortcode: 'dare',     url_patterns: [ 'http://dare.ht.lu.se/places/' ], color: '#ff7f0e' },
      //   { shortcode: 'dpp',      url_patterns: [ 'http://dpp.oeaw.ac.at/places/dpp_places.xls#}' ], color: '#efca10' },
      //   { shortcode: 'geonames', url_patterns: [ 'http://sws.geonames.org/'], color: '#2ca02c' },
      //   { shortcode: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ], color: '#1f77b4' },
      //   { shortcode: 'moeml',    url_patterns: [ 'http://mapoflondon.uvic.ca/{{id}}.htm' ], color: '#8c564b' }
      // ],

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
      },

      /** Helper to set default options **/
      defaultOpt = function(opts, key, defaultValue) {
        return (opts) ? ((opts[key]) ? opts[key] : defaultValue) : defaultValue;
      },

      /** Common code for getDistinctPlaceNames and getDistinctRecordNames **/
      getDistinctNames = function(titles, names, opts) {
        var split = defaultOpt(opts, 'split', false),
            excludeTitles = defaultOpt(opts, 'excludeTitles', false),

            nameLabels = jQuery.map(names, function(n) { return n.name; }),

            splitLabels = function(labels) {
              var result = [];

              jQuery.each(labels, function(idx, label) {
                var split = label.split(/,|\//);
                result = result.concat(split);
              });

              return result;
            },

            distinct = function(labels) {
              var distinct = {},

                  compareCount = function(a, b) {
                    return b.count - a.count;
                  };

              jQuery.each(labels, function(idx, label) {
                var trimmed = label.trim(),
                    count = distinct[trimmed];

                if (count)
                  distinct[trimmed] = count + 1;
                else
                  distinct[trimmed] = 1;
              });

              return jQuery.map(distinct, function(count, label) {
                return { label: label, count: count };
              }).sort(compareCount).map(function(t) {
                return t.label;
              });
            },

            removeTitles = function(distinct, titles) {
              var withoutTitles = [];

              jQuery.each(distinct, function(idx, name) {
                if (titles.indexOf(name) === -1)
                  withoutTitles.push(name);
              });

              return withoutTitles;
            },

            allLabels = (split) ? splitLabels(titles.concat(nameLabels)) : titles.concat(nameLabels);

        if (excludeTitles) {
          if (split)
            titles = splitLabels(titles);
          return removeTitles(distinct(allLabels), titles);
        } else {
          return distinct(allLabels);
        }
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

    /** Returns the URIs of all records conflated in this place **/
    getURIs: function(place) { return mapConflated(place, 'uri'); },

    /**
     * Returns the titles from all records conflated in this place
     *
     * If includeCountry is set to true, the country name will be appended like
     * so: {title}, {country name}
     */
    getTitles: function(place, includeCountry) {

          // Helper to get titles and country codes
      var getTitlesWithCountryCodes = function(place) {
            var tuples = [];

            jQuery.each(place.is_conflation_of, function(idx, record) {
              var title = record.title,
                  ccode = record.country_code;

              if (ccode)
                tuples.push({ title: title, country_code: ccode });
              else
                tuples.push({ title: title });
            });

            return tuples;
          };

      if (includeCountry) {
        return jQuery.map(getTitlesWithCountryCodes(place), function(tuple) {
          if (tuple.country_code)
            return tuple.title + ', ' + Countries.getName(tuple.country_code);
          else
            return tuple.title;
        });
      } else {
        return mapConflated(place, 'title');
      }
    },

    /** Returns the descriptions from all records conflated in this place **/
    getDescriptions: function(place) { return mapConflated(place, 'descriptions'); },

    /** Returns the country codes from all records conflated in this place **/
    getCountryCodes: function(place) { return mapConflated(place, 'country_code'); },

    /**
     * Returns a list of distinct names (ignoring language) contained in this place,
     * sorted by frequency.
     *
     * Options:
     *   split         - if set to true, names will be split on comma and / (default = false)
     *   excludeTitles - if set to true, names that already appear as title will be excluded
     *                   (default = false)
     */
    getDistinctPlaceNames: function(place, opts) {
      var titles = mapConflated(place, 'title'),
          names = mapConflated(place, 'names');
      return getDistinctNames(titles, names, opts);
    },

    /** Same functionality as getDistinctPlaceNames, but for a single gazetteer record **/
    getDistinctRecordNames: function(record, opts) {
      return getDistinctNames([ record.title ], record.names, opts);
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

    getBBoxSize: function(place) {
      if (!place.representative_geometry)
        return 0;

      if (place.representative_geometry.type === 'Point')
        return 0;

      var bbox = place.bbox.coordinates;
      return Math.abs(bbox[1][0] - bbox[0][0]) *
        Math.abs(bbox[1][1] - bbox[0][1]);
    },

    /**
     * Parses a gazetteer URI and determines the appropriate gazetteer
     * shortcode, ID, and signature color.
     */
    parseURI: function(uri) {
      var parseResult = { uri: uri },

          uriToId = function(uri, pattern) {
            var tokenPos = pattern.indexOf('\{\{id\}\}'),
                suffix;

            if (tokenPos === -1) {
              // Suffix
              return uri.substring(pattern.length);
            } else {
              // Infix
              suffix = pattern.substring(tokenPos + 6);
              return uri.substring(tokenPos, uri.length - suffix.length);
            }
          };

      jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
        var cont = true;
        jQuery.each(g.url_patterns, function(j, pattern) {
          var tokenPos = pattern.indexOf(TOKEN),
              patternSuffix = (tokenPos === -1) ? pattern : pattern.substring(0, tokenPos);

          if (uri.indexOf(patternSuffix) === 0) {
            parseResult.shortcode = g.shortcode;
            parseResult.id = uriToId(uri, pattern);
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
