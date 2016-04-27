define(function() {

  var PlaceUtils = {

    getBestMatchingRecord: function(place, name) {
      return place.is_conflation_of[0];
    },

    getRecord: function(place, uri) {
      var matchingRecords = jQuery.grep(place.is_conflation_of, function(record) {
        return record.uri === uri;
      });

      if (matchingRecords.length > 0)
        return matchingRecords[0];
      else
        return false;
    },

    getLabels: function(gazetteerRecord) {
      var labels = {},
          asArray = [],

           add = function(name) {
             jQuery.each(name.split(',|/'), function(idx, label) {
               var count = labels[label];
               if (count)
                 labels[label] = count + 1;
               else
                 labels[label] = 1;
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

       return jQuery.map(asArray, function(val) {
         return val[0];
       });
    }

  };

  return PlaceUtils;

});
