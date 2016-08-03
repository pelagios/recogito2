define([
  'document/annotation/common/editor/sections/place/cards/baseCard',
  'common/config'], function(Card, Config) {

  var NoMatchCard = function(containerEl, verificationStatus, lastModified) {

    // TODO cover the case of yellow place status'es - different message + place overlay

    var element = (Config.writeAccess) ? jQuery(
          '<div class="info-text">' +
            '<div class="no-match">' +
              '<div class="label"></div>' +
              '<button class="btn tiny change">Search</button> ' +
              '<button class="btn tiny delete icon">&#xf014;</button>' +
            '</div>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="info-text">' +
            '<div class="no-match readonly">' +
              '<div class="label"></div>' +
            '</div>' +
          '</div>'),

        labelEl = element.find('.label'),

        labelNoAutoMatch =
          '<h3>No automatic match found</h3>',
        labelFlagged =
          '<h3>Flagged as Not Identifiable</h3>' +
          '<p>A place no-one could resolve yet</p>',

        overlayFlagged =
          '<div class="map-overlay flagged">' +
            '<span class="icon">&#xe842;</span>' +
          '</div>',

        render = function() {
          containerEl.html(element);

          if (verificationStatus.value === 'NOT_IDENTIFIABLE') {
            labelEl.html(labelFlagged);
            containerEl.append(overlayFlagged);
          } else {
            labelEl.html(labelNoAutoMatch);
          }
        };

    this.render = render;

    Card.apply(this, [ element ]);

    render();
  };
  NoMatchCard.prototype = Object.create(Card.prototype);

  return NoMatchCard;

});
