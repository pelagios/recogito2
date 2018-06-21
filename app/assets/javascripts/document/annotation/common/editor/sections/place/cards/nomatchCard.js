define([
  'document/annotation/common/editor/sections/place/cards/baseCard',
  'common/config'], function(Card, Config) {

  var NoMatchCard = function(containerEl, verificationStatus, lastModified) {

    // TODO cover the case of yellow place status'es - different message + place overlay

    var element = (Config.writeAccess) ? jQuery(
          '<div class="info-text">' +
            '<div class="no-match">' +
              '<div class="label"></div>' +
              '<button class="btn tiny change" title="Use advanced search to find a match">Search</button> ' +
              '<button class="btn tiny flag icon" title="Flag this place as unidentified">&#xf11d;</button> ' +
              '<button class="btn tiny delete icon" title="Not a place - remove">&#xf014;</button>' +
            '</div>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="info-text">' +
            '<div class="no-match readonly">' +
              '<div class="label"></div>' +
            '</div>' +
          '</div>'),

        labelEl = element.find('.label'),

        btnFlag = element.find('.flag'),

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

          if (verificationStatus && verificationStatus.value === 'NOT_IDENTIFIABLE')
            setFlagged();
          else
            labelEl.html(labelNoAutoMatch);
        },

        setFlagged = function() {
          labelEl.html(labelFlagged);
          btnFlag.remove();
          containerEl.append(overlayFlagged);
        };

    this.render = render;
    this.setFlagged = setFlagged;

    Card.apply(this, [ element ]);

    render();
  };
  NoMatchCard.prototype = Object.create(Card.prototype);

  return NoMatchCard;

});
