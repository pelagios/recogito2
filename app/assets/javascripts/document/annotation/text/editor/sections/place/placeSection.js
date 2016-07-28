define([
  'document/annotation/text/editor/sections/place/cards/errorCard',
  'document/annotation/text/editor/sections/place/cards/nomatchCard',
  'document/annotation/text/editor/sections/place/cards/standardCard',
  'document/annotation/text/editor/sections/place/minimap',
  'document/annotation/text/editor/sections/section',
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/api',
  'common/config'], function(ErrorCard, NoMatchCard, StandardCard, MiniMap, Section, Formatting, PlaceUtils, API, Config) {

  var PlaceSection = function(parent, placeBody, quote) {
    var self = this,

        element = (function() {
          var el = jQuery(
            '<div class="section place">' +
              '<div class="map"></div>' +
              '<div class="info"></div>' +
            '</div>');

          parent.append(el);
          return el;
        })(),

        /** DOM shorthands **/
        mapEl  = element.find('.map'),
        infoEl = element.find('.info'),

        /** Minimap **/
        map   = new MiniMap(mapEl),

        /** 'Card' showing place info (depends on body & may change on updates) **/
        card  = false,

        /** Changes to be applied to the annotation when the user clicks OK **/
        queuedUpdates = [],

        /** Renders the standard place card with resolved gazetteer record **/
        renderStandardCard = function(record, verificationStatus, lastModified, opt_coord) {
          var latLon = (opt_coord) ? [opt_coord[1], opt_coord[0]] : false;
          card = new StandardCard(infoEl, record, verificationStatus, lastModified, !opt_coord);
          map.setLocation(latLon);
        },

        /** Renders a 'no match' place card, due to yellow status or failed match **/
        renderNoMatchCard = function(verificationStatus, lastModified) {
          card = new NoMatchCard(infoEl, verificationStatus, lastModified);
          map.clear();
        },

        /** Renders the error edge cases where the place body has a URI that can't be resolved **/
        renderResolveErrorCard = function(uri, verificationStatus, lastModified) {
          card = new ErrorCard(infoEl, uri, verificationStatus, lastModified);
          map.clear();
        },

        /** Fills the template with the appropriate place card **/
        fillTemplate = function(gazetteerRecordOrURI, verificationStatus, lastModified, opt_coord) {
          if (gazetteerRecordOrURI) {
            if (jQuery.type(gazetteerRecordOrURI) === 'string') {
              // A URI, but no record for it!? - edge case
              renderResolveErrorCard(gazetteerRecordOrURI, verificationStatus, lastModified);
            } else {
              renderStandardCard(gazetteerRecordOrURI, verificationStatus, lastModified, opt_coord);
            }
          } else {
            // No record or URI - render the 'no match' card
            renderNoMatchCard(verificationStatus, lastModified);
          }
        },

        /** Fills the place card based on the URI contained in the place body **/
        fillFromURI = function(uri, verificationStatus, lastModified) {
          API.getPlace(uri).done(function(place) {
            var record = PlaceUtils.getRecord(place, uri),
                coord = place.representative_point;
            fillTemplate(record, verificationStatus, lastModified, coord);
          }).fail(function(error) {
            fillTemplate(false, verificationStatus, lastModified);
          });
        },

        /** Fills the place card based on a search on the provided quote string **/
        fillFromQuote = function(quote, verificationStatus, lastModified) {
          API.searchPlaces(quote).done(function(response) {
            if (response.total > 0) {
              var topPlace = response.items[0],
                  bestRecord = PlaceUtils.getBestMatchingRecord(topPlace),
                  coord = topPlace.representative_point;

              placeBody.uri = bestRecord.uri;
              fillTemplate(bestRecord, verificationStatus, lastModified, coord);
            } else {
              fillTemplate(false, verificationStatus, lastModified);
            }
          });
        },

        /** Updates the section with a change performed by the user **/
        update = function(diff) {
          var lastModified = { by: Config.me, at: new Date() };

          // Diffs contain uri and status info
          if (placeBody.uri !== diff.uri && diff.uri) {
            // There's a new URI - update the place card
            fillFromURI(diff.uri, diff.status, lastModified);
          } else if (!diff.uri) {
            // There's no URI now (but there was one before!) - change to 'No Match' card
            renderNoMatchCard(diff.status, lastModified);
          }

          // Queue these updates for deferred storage
          queuedUpdates.push(function() {
            delete placeBody.last_modified_by;
            delete placeBody.last_modified_at;
            placeBody.status = diff.status;
            if (diff.uri)
              placeBody.uri = diff.uri;
            else
              delete placeBody.uri;
          });
        },

        /** Handles 'confirm' click **/
        onConfirm = function() {
          // Apply changes to card immediately...
          card.setLastModified({ by: Config.me, at: new Date() });
          card.setConfirmed();

          // ...but queue the actual change to the model for later
          queuedUpdates.push(function() { placeBody.status.value = 'VERIFIED'; });
        },

        hasChanged = function() {
          return queuedUpdates.length > 0;
        },

        /** Commits queued changes **/
        commit = function() {
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });
        },

        destroy = function() {
          element.remove();
        },

        init = function() {
          var lastModified = { by: placeBody.last_modified_by, at: placeBody.last_modified_at };

          if (placeBody.uri)
            // Resolve the URI contained in the annotation body
            fillFromURI(placeBody.uri, placeBody.status, lastModified);
          else if (placeBody.status.value === 'UNVERIFIED')
            // No URI - if the annotation is still UNVERIFIED, fetch a suggestion
            fillFromQuote(quote, placeBody.status, lastModified);
          else
            renderNoMatchCard(placeBody.status, lastModified);
        };

    init();

    element.on('click', '.unverified-confirm', onConfirm);

    // Change and delete events are simple passed on
    element.on('click', '.unverified-change, .change', function() { self.fireEvent('change'); });
    element.on('click', '.delete', function() { self.fireEvent('delete'); });

    this.body = placeBody;

    this.update = update;
    this.hasChanged = hasChanged;
    this.commit = commit;
    this.destroy = destroy;

    Section.apply(this);
  };
  PlaceSection.prototype = Object.create(Section.prototype);

  return PlaceSection;

});
