define([
  'common/ui/formatting',
  'common/config',
  'document/annotation/common/editor/sections/section'
], function(Formatting, Config, Section) {

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
                '</div>'),

              el = jQuery(
                '<div class="section category person">' +
                  '<div class="category-icon">&#xe863;</div>' +
                  '<div class="info"><span>Marked as a Person</span></div>' +
                '</div>');

          if (personBody.last_modified_by) {
            var infoEl = el.find('.info');
            infoEl.append(lastModified);

            if (Config.writeAccess)
              infoEl.append('<button class="btn tiny delete icon">&#xf014;</button>');
          }

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
