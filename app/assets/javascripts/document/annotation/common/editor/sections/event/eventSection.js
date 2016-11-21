define([
  'document/annotation/common/editor/sections/section',
  'common/ui/formatting'
], function(Section, Formatting) {

  /**
   * TODO almost completely redundant with PersonSection - but things will likely deviate
   * in the future. So leave this for now.
   */
  var EventSection = function(parent, eventBody) {
    var self = this,

        element = (function() {
          var lastModified = jQuery(
                '<div class="last-modified">' +
                  '<a class="by" href="/' + eventBody.last_modified_by + '">' +
                    eventBody.last_modified_by + '</a>' +
                  '<span class="at">' +
                    Formatting.timeSince(eventBody.last_modified_at) +
                  '</span>' +
                '</div>' +
                '<button class="btn tiny delete icon">&#xf014;</button>'),

              el = jQuery(
                '<div class="section category event">' +
                  '<div class="category-icon">&#xf005;</div>' +
                  '<div class="info"><span>Marked as an Event</span></div>' +
                '</div>');

          if (eventBody.last_modified_by)
            el.find('.info').append(lastModified);

          parent.append(el);
          return el;
        })(),

        destroy = function() {
          element.remove();
        };

    element.on('click', '.delete', function() { self.fireEvent('delete'); });

    this.body = eventBody;
    this.destroy = destroy;
    this.hasChanged = function() { return false; };
    this.commit = function() {}; // Not (yet) needed

    Section.apply(this);
  };
  EventSection.prototype = Object.create(Section.prototype);

  return EventSection;

});
