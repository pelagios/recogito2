define([], function() {

  var tags = [],

      /** Shorthand: adds the term to the tags array if it doesn't exist already **/
      addIfNotExists = function(term) {
        if (tags.indexOf(term) < 0)
          tags.push(term);
      };

  return {

    init : function(annotations) {
      var withRelations = annotations.filter(function(a) {
            return a.relations && a.relations.length > 0;
          });

      withRelations.forEach(function(annotation) {
        annotation.relations.forEach(function(relation) {
          var bodyValues = relation.bodies.map(function(b) {
                return b.value;
              });

          bodyValues.forEach(addIfNotExists);
        });
      });
    },

    add : addIfNotExists,

    tags : tags

  };

});
