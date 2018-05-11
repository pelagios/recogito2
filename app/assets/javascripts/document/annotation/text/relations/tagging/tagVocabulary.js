define([], function() {

  // Just for testing right now...
  var tags = [];

  return {
    add : function(term) {
      if (tags.indexOf(term) < 0)
        tags.push(term);
    },

    tags : tags
  };

});
