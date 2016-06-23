define([
  'document/annotation/text/editor/sections/section',
  'common/utils/formattingUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config'], function(Section, Formatting, PlaceUtils, API, Config) {

  var SLIDE_DURATION = 200;

  var PlaceSection = function(parent, placeBody, quote) {
    var self = this,

        element = (function() {
          var el = jQuery(
            '<div class="section place">' +
              '<div class="map"></div>' +
              '<div class="panel-container">' +
                '<div class="panel place-details">' +
                '</div>' +
                '<div class="warning-unlocated">' +
                  '<span class="icon">&#xf29c;</span>' +
                  '<span class="caption">NO LOCATION</span>' +
                '</div>' +
              '</div>' +
            '</div>');

          parent.append(el);
          return el;
        })(),

        standardTemplate = (function() {
          var el = (Config.writeAccess) ? jQuery(
            '<div>' +
              '<h3></h3>' +
              '<p class="gazetteer"></p>' +
              '<p class="description"></p>' +
              '<p class="names"></p>' +
              '<p class="date"></p>' +
              '<div class="created">' +
                '<a class="by"></a>' +
                '<span class="at"></span>' +
              '</div>' +
              '<div class="edit-buttons">' +
                '<button class="change btn tiny">Change</button>' +
                '<button class="delete btn tiny icon">&#xf014;</button>' +
              '</div>' +
              '<div class="warning-unverified">' +
                '<span class="warning"><span class="icon">&#xf071;</span> Automatic Match</span>' +
                '<button class="delete icon">&#xf014;</button>' +
                '<button class="unverified-change">Change</button>' +
                '<button class="unverified-confirm">Confirm</button>' +
              '</div>' +
            '</div>') : jQuery(
              // Simplified version for read-only mode
              '<div>' +
                '<h3></h3>' +
                '<p class="gazetteer"></p>' +
                '<p class="description"></p>' +
                '<p class="names"></p>' +
                '<p class="date"></p>' +
                '<div class="created">' +
                  '<a class="by"></a>' +
                  '<span class="at"></span>' +
                '</div>' +
                '<div class="warning-unverified">' +
                  '<span class="warning"><span class="icon">&#xf071;</span> Automatic Match</span>' +
                '</div>' +
              '</div>'
            );

          el.find('.created, .edit-buttons').hide();
          return el;
        })(),

        noMatchTemplate = (Config.writeAccess) ? jQuery(
          '<div class="no-match">' +
            '<h3>No automatic match found</h3>' +
            '<button class="change">Search</button> ' +
            '<button class="delete icon">&#xf014;</button>' +
          '</div>') : jQuery(
          // Simplified version for read-only mode
          '<div class="no-match">' +
            '<h3>No automatic match found</h3>' +
          '</div>'),

        panelContainer = element.find('.panel-container'),
        panel = element.find('.panel'),

        title = standardTemplate.find('h3'),
        gazetteerId = standardTemplate.find('.gazetteer'),
        description = standardTemplate.find('.description'),
        names = standardTemplate.find('.names'),
        date = standardTemplate.find('.date'),

        createdSection = standardTemplate.find('.created, .edit-buttons'),
        createdBy = standardTemplate.find('.by'),
        createdAt = standardTemplate.find('.at'),

        warningUnverified = standardTemplate.find('.warning-unverified'),
        warningUnlocated = standardTemplate.find('.warning-unlocated'),

        currentGazetteerRecord = false,

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

        formatGazetteerURI = function(uri) {
          var data = PlaceUtils.parseURI(uri);
          if (data.shortcode)
            return '<a class="gazetteer-id" href="' + uri + '" target="_blank">' +
                     data.shortcode + ':' + data.id +
                   '</a>';
          else
            return '<a class="gazetteer-id" href="' + uri + '" target="_blank">' +
                     uri +
                   '</a>';
        },

        /** Renders the standard place card with gazetteer record **/
        renderStandardCard = function(gazetteerRecord, status, opt_coord) {
          var latLon = (opt_coord) ? [opt_coord[1], opt_coord[0]] : false;

          panel.html(standardTemplate);

          title.html(gazetteerRecord.title);
          gazetteerId.html(formatGazetteerURI(gazetteerRecord.uri));

          if (gazetteerRecord.descriptions) {
            description.html(gazetteerRecord.descriptions[0].description);
            description.show();
          } else {
            description.empty();
            description.hide();
          }

          // names.htmlcreatedBy(labels.slice(1).join(', '));

          if (gazetteerRecord.temporal_bounds) {
            date.html(Formatting.yyyyMMddToYear(gazetteerRecord.temporal_bounds.from) + ' - ' +
                      Formatting.yyyyMMddToYear(gazetteerRecord.temporal_bounds.to));
            date.show();
          } else {
            date.empty();
            date.hide();
          }

          setCreated();

          if (status.value === 'UNVERIFIED') {
            createdSection.hide();
            warningUnverified.show();
          } else {
            createdSection.show();
            warningUnverified.hide();
          }

          // Map
          markerLayer.clearLayers();
          if (latLon) {
            panelContainer.removeClass('unlocated');
            L.marker(latLon).addTo(markerLayer);
            setCenter(latLon);
          } else {
            setCenter([37.98, 23.73]);
            panelContainer.addClass('unlocated');
          }
        },

        setCreated = function() {
          if (lastModified.by) {
            createdBy.html(lastModified.by);
            createdBy.attr('href', '/' + lastModified.by);
          }

          if (lastModified.at)
            createdAt.html(Formatting.timeSince(lastModified.at));
        },

        /** Renders a 'no match' place card, due to yellow status or failed match **/
        renderNoMatchCard = function(status) {
          // TODO implement
          panel.html(noMatchTemplate);
          setCenter([37.98, 23.73]);
        },

        /** Renders the error edge cases where the place body has a URI that can't be resolved **/
        renderResolveErrorCard = function() {
          gazetteerId.html(Formatting.formatGazetteerURI(placeBody.uri));

          if (placeBody.last_modified_by) {
            createdBy.html(placeBody.last_modified_by);
            createdBy.attr('href', '/' + placeBody.last_modified_by);
          }

          if (placeBody.last_modified_at)
            createdAt.html(Formatting.timeSince(placeBody.last_modified_at));

          createdSection.show();
          warningUnverified.hide();

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

            currentGazetteerRecord = record;
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
              currentGazetteerRecord = bestRecord;
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
          setCreated();

          createdSection.fadeIn(SLIDE_DURATION);
          warningUnverified.slideUp(SLIDE_DURATION);

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
