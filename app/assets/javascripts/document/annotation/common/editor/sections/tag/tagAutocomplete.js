define([], function() {
  
  var TagAutocomplete = function(parent, textarea, annotations) {

    var init = function() {
          var prefixMatcher = function(query, responseFn) {
                var matches = [],
                    qLow = query.toLowerCase();

                annotations.listUniqueTags().forEach(function(tag) {
                  if (tag.toLowerCase().indexOf(qLow) === 0)
                    matches.push(tag);
                });

                responseFn(matches);
              };

          textarea.typeahead({
            hint:false
          },{
            source: prefixMatcher
          });
        },

        hide = function() {
          textarea.typeahead('val', '').typeahead('close');
        };

    init();

    this.hide = hide;
  };

  return TagAutocomplete;

});
