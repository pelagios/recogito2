define([
  'document/annotation/common/editor/sections/place/cards/errorCard',
  'document/annotation/common/editor/sections/place/cards/nomatchCard',
  'document/annotation/common/editor/sections/place/cards/standardCard',
  'document/annotation/common/editor/sections/place/minimap',
  'document/annotation/common/editor/sections/section',
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config'
], function(
  ErrorCard,
  NoMatchCard,
  StandardCard,
  MiniMap,
  Section,
  Formatting,
  AnnotationUtils,
  PlaceUtils,
  API,
  Config
) {

  var PlaceSection = function(parent, placeBody, opt_toponym, allAnnotations) {
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

        /** Fills the place card based on a search on the provided toponym string **/
        fillFromToponym = function(toponym, verificationStatus, lastModified) {

          // Checks existing annotations for verfied matches on the same
          // toponym and returns the URI that was associated with it most
          // recently
          var mostRecentPreviousMatch = (function() {
                // PLACE bodies for previous matches on this toponym
                var previousMatches = [];

                allAnnotations.forEach(function(a) {
                  var quote = AnnotationUtils.getQuote(a),
                      placeBodies = AnnotationUtils.getBodiesOfType(a, 'PLACE'),
                      verifiedPlaceBodies = placeBodies.filter(function(b) {
                        return b.status && b.status.value === 'VERIFIED';
                      });

                  // TODO images

                  if (quote && quote.toLowerCase() === toponym.toLowerCase() && verifiedPlaceBodies.length > 0)
                    previousMatches = previousMatches.concat(verifiedPlaceBodies);
                });

                previousMatches.sort(function(a, b) {
                  if (a.last_modified_at < b.last_modified_at) return 1;
                  if (a.last_modified_at > b.last_modified_at) return -1;
                  return 0;
                });

                if (previousMatches.length > 0)
                  return previousMatches[0].uri;
              })();

          if (mostRecentPreviousMatch)
            fillFromURI(mostRecentPreviousMatch, verificationStatus, lastModified);
          else
            API.searchPlaces(toponym).done(function(response) {
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

        /** Handles 'flag' click **/
        onFlag = function() {
          card.setFlagged();
          queuedUpdates.push(function() {
            placeBody.status.value = 'NOT_IDENTIFIABLE';
          });
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
          else if (opt_toponym && placeBody.status.value === 'UNVERIFIED')
            // No URI - if the annotation is still UNVERIFIED, fetch a suggestion
            fillFromToponym(opt_toponym, placeBody.status, lastModified);
          else
            renderNoMatchCard(placeBody.status, lastModified);
        };

    init();

    element.on('click', '.unverified-confirm', onConfirm);
    element.on('click', '.flag', onFlag);

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
