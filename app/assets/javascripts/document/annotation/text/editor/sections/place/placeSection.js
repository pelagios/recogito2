define([
  'document/annotation/text/editor/sections/place/cards/errorCard',
  'document/annotation/text/editor/sections/place/cards/nomatchCard',
  'document/annotation/text/editor/sections/place/cards/standardCard',
  'document/annotation/text/editor/sections/section',
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/api',
  'common/config'], function(ErrorCard, NoMatchCard, StandardCard, Section, Formatting, PlaceUtils, API, Config) {

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

        infoEl = element.find('.info'),

        card = false,

        lastModified = {
          by: placeBody.last_modified_by,
          at: placeBody.last_modified_at
        },

        /** Changes to be applied to the annotation when the user clicks OK **/
        queuedUpdates = [],

        awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(element.find('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 4,
          zoomControl: false,
          layers: [ awmc ]
        }),

        markerLayer = L.layerGroup().addTo(map),

        setCenter = function(latLon) {
          var centerPt, centerLatLng;

          // Need to set initial center before we can offset
          map.invalidateSize();
          map.setView(latLon, 4, { animate: false });

          centerPt = L.point(82, 70);
          centerLatLng = map.containerPointToLatLng(centerPt);

          map.panTo(centerLatLng, { animate: false});
        },

        /** Renders the standard place card with gazetteer record **/
        renderStandardCard = function(record, verificationStatus, opt_coord) {
          var latLon = (opt_coord) ? [opt_coord[1], opt_coord[0]] : false;
          card = new StandardCard(infoEl, record, verificationStatus, lastModified);
          markerLayer.clearLayers();
          if (latLon) {
            // panelContainer.removeClass('unlocated');
            L.marker(latLon).addTo(markerLayer);
            setCenter(latLon);
          } else {
            setCenter([37.98, 23.73]);
            // panelContainer.addClass('unlocated');
          }
        },

        /** Renders a 'no match' place card, due to yellow status or failed match **/
        renderNoMatchCard = function(verificationStatus) {
          card = new NoMatchCard(infoEl, verificationStatus, lastModified);
          setCenter([37.98, 23.73]);
        },

        /** Renders the error edge cases where the place body has a URI that can't be resolved **/
        renderResolveErrorCard = function() {
          card = new ErrorCard(inforEl, record, verificationStatus, lastModified);
          setCenter([37.98, 23.73]);
        },

        /** Fills the template by delegating to the appropriate place card renderer **/
        fillTemplate = function(gazetteerRecordOrURI, status, opt_coord) {
          if (gazetteerRecordOrURI) {
            if (jQuery.type(gazetteerRecordOrURI) === 'string') {
              // A URI, but no record for it!? - edge case
              renderResolveErrorCard();
            } else {
              renderStandardCard(gazetteerRecordOrURI, status, opt_coord);
            }
          } else {
            // No record or URI - render the 'no match' card
            renderNoMatchCard(status);
          }
        },

        /** Fills the place card based on the URI contained in the place body **/
        fillFromURI = function(uri, status) {
          jQuery.getJSON('/api/places/' + encodeURIComponent(uri), function(place) {
            var record = PlaceUtils.getRecord(place, uri),
                coord = place.representative_point;

            fillTemplate(record, status, coord);
          }).fail(function(error) {
            fillTemplate(false, status, false);
          });
        },

        /** Fills the place card based on a search on the provided quote string **/
        fillFromQuote = function(quote, status) {
          API.searchPlaces(quote).done(function(response) {
            if (response.total > 0) {
              var topPlace = response.items[0],
                  bestRecord = PlaceUtils.getBestMatchingRecord(topPlace),
                  coord = topPlace.representative_point;

              placeBody.uri = bestRecord.uri;
              fillTemplate(bestRecord, status, coord);
            } else {
              fillTemplate(false, status, false);
            }
          });
        },

        update = function(diff) {
          lastModified = { by: Config.me, at: new Date() };

          // Diffs contain uri and status info
          if (placeBody.uri !== diff.uri && diff.uri) {
            // There's a new URI - update the place card
            fillFromURI(diff.uri, diff.status);
          } else if (!diff.uri){
            // There's no URI now (but there was one before!) - change to 'No Match' card
            renderNoMatchCard(diff.status);
          }

          // Queue updates to the model for later
          queuedUpdates.push(function() {
            delete placeBody.last_modified_by;
            delete placeBody.last_modified_at;
            placeBody.uri = diff.uri;
            placeBody.status = diff.status;
          });
        },

        onConfirm = function() {
          // Apply UI changes now
          lastModified = { by: Config.me, at: new Date() };
          card.setLastModified(lastModified);
          card.setConfirmed();

          // Queue updates to model for later
          queuedUpdates.push(function() {
            placeBody.status.value = 'VERIFIED';
          });
        },

        onChange = function() {
          self.fireEvent('change');
        },

        onDelete = function() {
          self.fireEvent('delete');
        },

        commit = function() {
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });
        },

        destroy = function() {
          element.remove();
        };

    element.on('click', '.unverified-confirm', onConfirm);
    element.on('click', '.unverified-change, .change', onChange);
    element.on('click', '.delete', onDelete);

    if (placeBody.uri)
      fillFromURI(placeBody.uri, placeBody.status);
    else
      fillFromQuote(quote, placeBody.status);

    this.update = update;

    this.body = placeBody;
    this.commit = commit;
    this.destroy = destroy;

    Section.apply(this);
  };
  PlaceSection.prototype = Object.create(Section.prototype);

  return PlaceSection;

});
