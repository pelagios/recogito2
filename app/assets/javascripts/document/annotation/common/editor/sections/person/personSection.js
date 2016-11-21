define([
  'document/annotation/common/editor/sections/section',
  'common/ui/formatting'
], function(Section, Formatting) {

  var PersonSection = function(parent, personBody, personName) {
    var self = this,

        element = (function() {
          var lastModified = jQuery(
                '<div class="last-modified">' +
                  '<a class="by" href="/' + personBody.last_modified_by + '">' +
                    personBody.last_modified_by + '</a>' +
                  '<span class="at">' +
                    Formatting.timeSince(personBody.last_modified_at) +
                  '</span>' +
                '</div>' +
                '<button class="btn tiny delete icon">&#xf014;</button>'),

              el = jQuery(
                '<div class="section category person">' +
                  '<div class="category-icon">&#xe863;</div>' +
                  '<div class="info"><span>Marked as a Person</span></div>' +
                '</div>');

          if (personBody.last_modified_by)
            el.find('.info').append(lastModified);

          parent.append(el);
          return el;
        })(),

        destroy = function() {
          element.remove();
        };

    element.on('click', '.delete', function() { self.fireEvent('delete'); });

    this.body = personBody;
    this.destroy = destroy;
    this.hasChanged = function() { return false; };
    this.commit = function() {}; // Not (yet) needed

    Section.apply(this);
  };
  PersonSection.prototype = Object.create(Section.prototype);

  return PersonSection;

});
