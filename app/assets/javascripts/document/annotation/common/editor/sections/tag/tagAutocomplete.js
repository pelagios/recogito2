define([
  'common/hasEvents',
], function(HasEvents) {
  
  var TagAutocomplete = function(textarea, annotations) {

    var self = this,

        init = function() {
          var prefixMatcher = function(query, responseFn) {
                var matches = [],
                    qLow = query.toLowerCase();
                
                annotations.listUniqueTags().forEach(function(tag) {
                  var label = tag.value || tag;
                  if (label.toLowerCase().indexOf(qLow) === 0)
                    matches.push(tag);
                });

                responseFn(matches);
              };

          textarea.typeahead({
            hint:false,
            minLength:0,
          },{
            source: prefixMatcher,
            limit:10,
            display: function(tag) {
              return tag.value || tag;
            },
            templates: {
              suggestion: function(tag) {
                return tag.value ? '<div>' + tag.value + '</div>' : '<div>' + tag + '</div>';
              }
            }
          });

          textarea.bind('typeahead:select', function(evt, suggestion) {
            evt.preventDefault();
            self.fireEvent('select', suggestion);
          });
        },

        hide = function() {
          textarea.typeahead('val', '').typeahead('close');
        };

    init();

    this.hide = hide;

    HasEvents.apply(this);
  };
  TagAutocomplete.prototype = Object.create(HasEvents.prototype);

  return TagAutocomplete;

});
