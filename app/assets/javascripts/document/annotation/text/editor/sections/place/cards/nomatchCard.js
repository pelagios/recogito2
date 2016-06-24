define([
  'document/annotation/text/editor/sections/place/cards/baseCard',
  'common/config'], function(Card, Config) {

  var NoMatchCard = function(containerEl, verificationStatus, lastModified) {

    // TODO cover the case of yellow place status'es - different message + place overlay

    var element = (Config.writeAccess) ? jQuery(
          '<div class="info-text">' +
            '<div class="no-match">' +
              '<h3>No automatic match found</h3>' +
              '<button class="btn tiny change">Search</button> ' +
              '<button class="btn tiny delete icon">&#xf014;</button>' +
            '</div>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="info-text">' +
            '<div class="no-match readonly">' +
              '<h3>No automatic match found</h3>' +
            '</div>' +
          '</div>'),

        render = function() {
          containerEl.html(element);
        };

    this.render = render;

    Card.apply(this, [ element ]);

    render();
  };
  NoMatchCard.prototype = Object.create(Card.prototype);

  return NoMatchCard;

});
