define([
  'document/annotation/text/editor/sections/place/cards/baseCard',
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/config'], function(Card, Formatting, PlaceUtils, Config) {

  var StandardCard = function(containerEl, record, verificationStatus, lastModified) {

    // TODO cover the case of unlocated places - map overlay!

    var self = this,

        SLIDE_DURATION = 200,

        element = (Config.writeAccess) ? jQuery(
          '<div class="info-text">' +
            '<div class="place-details">' +
              '<h3 class="title"></h3>' +
              '<p class="uris"></p>' +
              '<p class="description"></p>' +
              '<p class="names"></p>' +
              '<p class="date"></p>' +
              '<div class="last-modified">' +
                '<a class="by"></a>' +
                '<span class="at"></span>' +
              '</div>' +
              '<div class="edit-buttons"></div>' +
              '<div class="unverified-warning"></div>' +
            '</div>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="info-text">' +
            '<div class="place-details">' +
              '<h3 class="title"></h3>' +
              '<p class="uris"></p>' +
              '<p class="description"></p>' +
              '<p class="names"></p>' +
              '<p class="date"></p>' +
              '<div class="last-modified">' +
                '<a class="by"></a>' +
                '<span class="at"></span>' +
              '</div>' +
              '<div class="unverified-warning readonly"></div>' +
            '</div>' +
          '</div>'),

        titleEl       = element.find('.title'),
        urisEl        = element.find('.uris'),
        descriptionEl = element.find('.description'),
        namesEl       = element.find('.names'),
        dateEl        = element.find('.date'),

        lastModifiedEl   = element.find('.last-modified'),
        lastModifiedByEl = element.find('.last-modified .by'),
        lastModifiedAtEl = element.find('.last-modified .at'),

        editButtonsEl       = element.find('.edit-buttons'),
        unverifiedWarningEl = element.find('.unverified-warning'),

        render = function() {
          titleEl.html(record.title);
          urisEl.html(self.formatURI(record.uri));

          if (record.descriptions)
            descriptionEl.html(record.descriptions[0].description);

          namesEl.html(PlaceUtils.getLabels(record, true).join(', '));

          if (record.temporal_bounds)
            dateEl.html(Formatting.yyyyMMddToYear(record.temporal_bounds.from) + ' - ' +
                      Formatting.yyyyMMddToYear(record.temporal_bounds.to));

          if (verificationStatus.value === 'UNVERIFIED') {
            lastModifiedEl.hide();
            if (Config.writeAccess)
              unverifiedWarningEl.html(Card.TEMPLATES.UNVERIFIED_WARNING_WRITE);
            else
              unverifiedWarningEl.html(Card.TEMPLATES.UNVERIFIED_WARNING_READ);
          } else {
            unverifiedWarningEl.hide();
            self.setLastModified(lastModified);
            if (Config.writeAccess)
              editButtonsEl.html(Card.TEMPLATES.EDIT_BUTTONS);
          }

          containerEl.html(element);
        },

        setConfirmed = function() {
          lastModifiedEl.fadeIn(SLIDE_DURATION);
          unverifiedWarningEl.slideUp(SLIDE_DURATION);
        };

    Card.apply(this, [ element ]);

    this.setConfirmed = setConfirmed;
    render();
  };
  StandardCard.prototype = Object.create(Card.prototype);

  return StandardCard;

});
