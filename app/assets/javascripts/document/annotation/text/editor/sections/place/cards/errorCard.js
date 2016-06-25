define([
  'document/annotation/text/editor/sections/place/cards/baseCard',
  'common/config'], function(Card, Config) {

  var ErrorCard = function(containerEl, record, verificationStatus, lastModified) {

    // TODO do we need to include the 'unverified' warning?

    var element = (Config.writeAccess) ? jQuery(
          '<div class="info-text">' +
            '<div class="place-details">' +
              '<p class="uris"></p>' +
              '<div class="last-modified">' +
                '<a class="by"></a>' +
                '<span class="at"></span>' +
              '</div>' +
              '<div class="edit-buttons"></div>' +
            '</div>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="info-text">' +
            '<div class="place-details">' +
              '<p class="uris"></p>' +
              '<div class="last-modified">' +
                '<a class="by"></a>' +
                '<span class="at"></span>' +
              '</div>' +
            '</div>' +
          '</div>'),

        editButtons =
          '<button class="change btn tiny">Change</button>' +
          '<button class="delete btn tiny icon">&#xf014;</button>',

        urisEl = element.find('.uris'),

        lastModifiedEl   = element.find('.last-modified'),
        lastModifiedByEl = element.find('.last-modified .by'),
        lastModifiedAtEl = element.find('.last-modified .at'),

        editButtonsEl       = element.find('.edit-buttons'),

        render = function() {
          urisEl.html(Card.formatURI(record.uri));
          self.setLastModified(lastModified);

          if (Config.writeAccess)
            editButtonsEl.html(editButtons);
        };

    this.render = render;
    
    Card.apply(this, [ element ]);
    render();
  };
  ErrorCard.prototype = Object.create(Card.prototype);

  return ErrorCard;

});
