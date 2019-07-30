plugins.PlaceStats = function(args) {

  var render = function(
        annotationsTotal,
        placesTotal,
        avgCharDistance,
        charDistanceStdDeviation,
        resolvedPlacesTotal,
        avgGeoDistance,
        geoDistnaceStdDeviation
      ) {

    return jQuery(
      '<table>' +
        '<tr><td>Places total</td><td>' + placesTotal + '</td></tr>' +
        '<tr><td>Average distance (characters):</td><td>' + avgCharDistance.toFixed(2) + '</td></tr>' +
        '<tr><td>Standard deviation</td><td>' + charDistanceStdDeviation.toFixed(2) + '</td></tr>' +
        '<tr><td>Resolved places total</td><td>' + resolvedPlacesTotal + '</td></tr>' +
        '<tr><td>Average centroid distance (km)</td><td>' + avgGeoDistance.toFixed(2) + '</td></tr>' +
        '<tr><td>Standard deviation</td><td>' + geoDistnaceStdDeviation.toFixed(2) + '</td></tr>' +
      '</table>');
  };
      
  var sum = function(arr) {
        return arr.reduce(function(s, el) {
          return s + el;
        }, 0);
      },

      distance = function(from, to) {
        var EARTH_RADIUS = 6371;

        var toRad = function(deg) {
              return deg * Math.PI / 180;
            };

        var dLon = toRad(to[0] - from[0]),
            dLat = toRad(to[1] - from[1]);

        var lat1 = toRad(from[1]),
            lat2 = toRad(to[1]);

        var a = 
          Math.sin(dLat / 2) * Math.sin(dLat / 2) +
          Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);

        var c = 
          2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
      },
  
      charOffset = function(annotation) {
        return parseInt(annotation.anchor.substr(12));
      },

      firstPlaceBody = function(annotation) {
        var placeBodies = annotation.bodies.filter(function(body) {
          return body.type === 'PLACE';
        });

        if (placeBodies.length > 0) 
          return placeBodies[0];
        else
          return null;
      }

      /** Returns true if all annotations are plaintext annotations **/
      isPlainTextDocument = function() {
        var nonPlaintextAnnotations = args.annotations.filter(function(a) {
          return a.anchor.indexOf('char-offset') === -1;
        });

        return nonPlaintextAnnotations.length === 0;
      },

      filterPlaces = function(annotations) {
        return annotations.filter(function(a) {
          var placeBodies = a.bodies.filter(function(b) {
            return b.type === 'PLACE';
          });

          return placeBodies.length > 0;
        });
      },

      /** This pre-supposes plaintext annotations! **/
      sortByCharOffset = function(annotations) {
        return annotations.sort(function(a, b) {
          return a.anchor.substr(12) - b.anchor.substr(12);
        });
      },

      /**
       * Iterates through the annotations and records distance 
       * between them, measured in characters.
       */
      getCharDistances = function(annotations) {
        return annotations.reduce(function(distances, a) {
          if (distances.length > 1) {
            var prevOffset = distances[distances.length - 1];
            var d = charOffset(a) - prevOffset;
            distances.push(d);
          } else {
            distances.push(charOffset(a));
          }

          return distances;
        }, []);
      },

      getStdDeviation = function(values, avg) {
        var variance = values.reduce(function(sum, value) {
          return sum + Math.pow(value - avg, 2);
        }, 0) / values.length;

        return Math.sqrt(variance);
      },

      resolvePlaces = function(annotations) {
        var getPlace = function(uri) {
              var match;

              args.entities.forEach(function(e) {
                e.is_conflation_of.forEach(function(record) {
                  if (record.uri === uri)
                    match = {
                      entity: e,
                      record: record
                    }
                });
              });

              return match;
            };

            maybeResolvedPlaces = annotations.map(function(a) {
              var placeBody = firstPlaceBody(a);
              if (placeBody) {
                return getPlace(placeBody.uri);
              } else {
                return null;
              }
            });

        return maybeResolvedPlaces.filter(function(el) {
          return el;
        });
      },

      placeToCentroid = function(p) {
        return (p.record.representative_point) ? p.record.representative_point : p.entity.representative_point;        
      },

      getGeoDistances = function(centroids) {
        var distances = [];

        for (var i = 1; i<centroids.length; i++) {
          var from = centroids[i - 1],
              to = centroids[i];
          distances.push(distance(from, to));
        }

        return distances;
      };

  if (isPlainTextDocument()) {
    var placeAnnotations = filterPlaces(args.annotations),

        sortedPlaceAnnotations = sortByCharOffset(placeAnnotations),
        charDistances = getCharDistances(sortedPlaceAnnotations),

        avgCharDistance = sum(charDistances) / sortedPlaceAnnotations.length, 
        charDistanceStdDeviation = getStdDeviation(charDistances, avgCharDistance),

        placesBySequence = resolvePlaces(sortedPlaceAnnotations);
        centroidsBySequence = placesBySequence.map(placeToCentroid).filter(function(el) { return el; });

        geoDistances = getGeoDistances(centroidsBySequence);

        avgGeoDistance = sum(geoDistances) / geoDistances.length;
        geoDistancesStdDeviation = getStdDeviation(geoDistances, avgGeoDistance);

    jQuery('#placestats').append(
      render(
        args.annotations.length,
        placeAnnotations.length,
        avgCharDistance, charDistanceStdDeviation,
        centroidsBySequence.length,
        avgGeoDistance, geoDistancesStdDeviation
      )
    );
} else {
    // TODO error message
  }
}