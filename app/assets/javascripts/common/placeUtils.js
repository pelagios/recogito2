define(function() {

  var PlaceUtils = {

    getLabels : function(place) {
      var labels = {},

          asArray = [],

          add = function(name) {
            jQuery.each(name.split(), function(idx, label) {
              var count = labels[label];
              if (count) {
                labels[label] = count + 1;
              } else {
                labels[label] = 1;
              }
            });
          };

      // Collect all names and their frequency
      jQuery.each(place.is_conflation_of, function(idx, record) {
        add(record.title);
        jQuery.each(record.names, function(idx, n) {
          add(n.name);
        });
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
