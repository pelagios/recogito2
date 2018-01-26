define([], function() {

  var TagAutocomplete = function(parent, textarea, annotations) {

    var init = function() {
          textarea.typeahead({
            hint:false
          },{
            source: function(query, results) {
              // TODO how do we access all unique tags on this page (without spagetthi code)?
              results(annotations.getUniqueTags());
            }
          });
        };

    init();
  };

  return TagAutocomplete;

});
