define([
  'document/annotation/common/georesolution/searchresultCard',
  'common/map/basemap',
  'common/ui/countries',
  'common/ui/formatting',
  'common/utils/placeUtils',
  'common/api',
  'common/hasEvents'
], function(ResultCard, BaseMap, Countries, Formatting, PlaceUtils, API, HasEvents) {

  var SHAPE_STYLE = {
        color:'#146593',
        fillColor:'#5397b0',
        opacity:1,
        weight:1.5,
        fillOpacity:0.7
      };

  var GeoresolutionPanel = function() {

    var self = this,

        element = (function() {
            var el = jQuery(
              '<div class="clicktrap">' +
                '<div class="modal-wrapper georesolution-wrapper">' +
                  '<div class="modal georesolution-panel">' +
                    '<div class="modal-header">' +
                      '<div class="georesolution-search">' +
                        '<input class="search inline" type="text" placeholder="Search for a Place..." />' +
                        '<button class="search icon">&#xf002;</button>' +
                      '</div>' +
                      '<div class="flag-this-place">' +
                        'Can\'t find the right match?' +
                        '<span class="flag">' +
                          '<button class="outline-icon nostyle">&#xe842;</button>' +
                          '<span class="label">Flag this place</span>' +
                        '</span>' +
                      '</div>' +
                      '<button class="nostyle outline-icon cancel">&#xe897;</button>' +
                    '</div>' +
                    '<div class="modal-body">' +
                      '<div class="georesolution-sidebar">' +
                        '<div class="result-header">'+
                          '<div class="result-total">' +
                            '<span class="icon">&#xf03a;</span> ' +
                            '<span class="label"></span> ' +
                            '<span class="result-took"></span>' +
                          '</div>' +
                        '</div>' +
                        '<div class="result-list">' +
                          '<ul></ul>' +
                          '<div class="wait-for-next">' +
                            '<img src="/assets/images/wait-circle.gif">' +
                          '</div>' +
                        '</div>' +
                      '</div>' +
                      '<div class="map-container">' +
                        '<div class="map"></div>' +
                        '<div class="map-controls">' +
                          '<div class="layers control icon" title="Change base layer">&#xf0c9;</div>' +
                          '<div class="zoom">' +
                            '<div class="zoom-in control" title="Zoom in">+</div>' +
                            '<div class="zoom-out control" title="Zoom out">&ndash;</div>' +
                          '</div>' +
                        '</div>' +
                      '</div>' +
                    '</div>' +
                  '</div>' +
                '</div>' +
              '</div>');

            el.find('.wait-for-next').hide();
            el.find('.modal-wrapper').draggable({ handle: '.modal-header' });
            el.hide();
            jQuery(document.body).append(el);
            return el;
          })(),

          searchInput     = element.find('.search'),

          btnFlag         = element.find('.flag'),
          btnCancel       = element.find('.cancel'),

          resultTotal     = element.find('.result-total .label'),
          resultTook      = element.find('.result-took'),

          resultContainer = element.find('.result-list'),
          resultList      = element.find('.result-list ul'),
          waitForNext     = element.find('.wait-for-next'),

          mapContainer    = element.find('.map-container'),

          placeBody = false,

          /** Either the value of the search field, or the 'fuzzified' version, with ~ appended **/
          currentSearch = false,

          currentSearchResults = [],
          currentSearchResultsTotal,

          /**
           * The map popup is handled by Leaflet, but we need keep track
           * of the 'unlocated place' popup ourselves
           */
          unlocatedPopup = false,

          map = new BaseMap(element.find('.map')),

          markerLayer = L.featureGroup(),

          shapeLayer = L.featureGroup(),

          closeUnlocatedPopup = function() {
            if (unlocatedPopup) {
              unlocatedPopup.remove();
              unlocatedPopup = false;
              mapContainer.removeClass('unlocated');
            }
          },

          openUnlocatedPopup = function(popup) {
            var wrapper = jQuery(
                  '<div class="unlocated popup-wrapper">' +
                    '<div class="place-popup-content"></div>' +
                  '</div>');

            closeUnlocatedPopup();
            map.leafletMap.closePopup();
            mapContainer.addClass('unlocated');

            unlocatedPopup = wrapper;
            popup.prepend('<button class="nostyle outline-icon close">&#xe897;</button>');
            wrapper.find('.place-popup-content').append(popup);
            popup.on('click', '.close', closeUnlocatedPopup);
            mapContainer.append(wrapper);
          },

          openMapPopup = function(popup, marker) {
            marker.unbindPopup();
            closeUnlocatedPopup();
            // TODO this fails for GeoJSON FeatureCollections
            // marker.bindPopup(popup[0]).openPopup();

            // TODO temporary fix, but not ideal, since it opens the popup at the
            // tip of the marker pin, rather than at the handle. Figure out a
            // solution that branches based on the type of marker
            marker.bindPopup(popup[0]).openPopup(marker.getBounds().getCenter());
          },

          openPopup = function(place, opt_marker) {
            var titles = PlaceUtils.getTitles(place, true),

                popup = jQuery(
                  '<div class="popup">' +
                    '<div class="popup-header">' +
                      '<h3>' + titles.join(', ') + '</h3>' +
                    '</div>' +
                    '<div class="popup-choices"><table class="gazetteer-records"></table></div>' +
                  '</div>');

            place.is_conflation_of.reduce(function(previousShortcode, record) {
              var recordId = PlaceUtils.parseURI(record.uri),

                  title = (record.country_code) ?
                    record.title + ', ' + Countries.getName(record.country_code) :
                    record.title,

                  names = PlaceUtils.getDistinctRecordNames(record, { excludeTitles: true }),

                  template = jQuery(
                    '<tr data-uri="' + recordId.uri + '">' +
                      '<td class="record-id">' +
                        '<span class="shortcode"></span>' +
                        '<span class="id"></span>' +
                      '</td>' +
                      '<td class="place-details">' +
                        '<h3>' + title + '</h3>' +
                        '<p class="names">' + names.join(', ') + '</p>' +
                        '<p class="description"></p>' +
                        '<p class="date"></p>' +
                      '</td>' +
                    '</tr>');

              if (recordId.shortcode) {
                template.find('.shortcode').html(recordId.shortcode);
                template.find('.id').html(recordId.id);
                template.find('.record-id').css('background-color', recordId.color);

                if (previousShortcode === recordId.shortcode)
                  template.find('.record-id').css('border-top', '1px solid rgba(0, 0, 0, 0.05)');
              }

              if (record.descriptions && record.descriptions.length > 0)
                template.find('.description').html(record.descriptions[0].description);
              else
                template.find('.description').hide();

              if (record.temporal_bounds)
                template.find('.date').html(
                  Formatting.yyyyMMddToYear(record.temporal_bounds.from) + ' - ' +
                  Formatting.yyyyMMddToYear(record.temporal_bounds.to));
              else
                template.find('.date').hide();

              popup.find('.popup-choices table').append(template);

              if (recordId.shortcode) return recordId.shortcode;
            }, false);

            popup.on('click', 'tr', function(e) {
              var tr = jQuery(e.target).closest('tr');
              self.fireEvent('change', placeBody, {
                uri: tr.data('uri'),
                status: { value: 'VERIFIED' }
              });
              close();
            });

            if (opt_marker)
              openMapPopup(popup, opt_marker);
            else
              openUnlocatedPopup(popup);
          },

          onFlag = function() {
            self.fireEvent('change', placeBody, {
              uri: false,
              status: { value: 'NOT_IDENTIFIABLE' }
            });
            close();
          },

          onNextPage = function(response) {
            var createMarker = function(place) {
                  if (place.representative_geometry && place.representative_geometry.type !== 'Point')
                    return L.geoJSON(place.representative_geometry, SHAPE_STYLE).addTo(shapeLayer);
                  else if (place.representative_point)
                    return L.marker([place.representative_point[1], place.representative_point[0]]).addTo(markerLayer);
                },

                moreAvailable =
                  response.total > currentSearchResults.length + response.items.length;

            // Switch wait icon on/off
            if (moreAvailable)
              waitForNext.show();
            else
              waitForNext.hide();

            currentSearchResults = currentSearchResults.concat(response.items);
            currentSearchResultsTotal = response.total;

            resultTotal.html(response.total + ' Total');
            resultTook.html('Took ' + response.took + 'ms');

            jQuery.each(response.items, function(idx, place) {
              var result = new ResultCard(resultList, place),
                  marker = createMarker(place);

              // Click on the list item opens popup (on marker, if any)
              result.on('click', function() { openPopup(place, marker); });

              // If there's a marker, click on the marker opens popup
              if (marker)
                marker.on('click', function(e) { openPopup(place, marker); });
            });
          },

          /** If scrolled to bottom, we load the next result page if needed **/
          onScroll = function(e) {
            var scrollPos = resultContainer.scrollTop() + resultContainer.innerHeight(),
                scrollBottom = resultContainer[0].scrollHeight;

            if (scrollPos >= scrollBottom)
              if (currentSearchResultsTotal > currentSearchResults.length)
                search(currentSearchResults.length);
          },

          search = function(opt_offset) {
            var offset = (opt_offset) ? opt_offset : 0,

                endsWith = function(str, char) {
                  return str.indexOf(char, str.length -  char.length) !== -1;
                },

                onResponse = function(response) {
                  // Try again with a fuzzy search
                  if (response.total === 0 && !endsWith(currentSearch, '~')) {
                    currentSearch = currentSearch + '~';
                    API.searchPlaces(currentSearch, offset).done(onNextPage);
                  } else {
                    onNextPage(response);
                  }
                };

            if (currentSearch)
              API.searchPlaces(currentSearch, offset).done(onResponse);
          },

          clear = function() {
            markerLayer.clearLayers();
            shapeLayer.clearLayers();
            resultList.empty();
            resultTotal.empty();
            resultTook.empty();
            currentSearchResults = [];
            currentSearchResultsTotal = 0;
          },

          open = function(toponym, body) {
            placeBody = body;

            searchInput.val(toponym);
            currentSearch = toponym;
            element.show();
            map.refresh();

            clear();
            search();

            searchInput.get(0).focus();
          },

          close = function() {
            placeBody = false;
            closeUnlocatedPopup();
            waitForNext.hide();
            element.hide();
          },

          /**
           * This prevents the background text from scrolling once the bottom of the
           * search result list is reached.
           */
          blockMouseWheelBubbling = function() {
            element.bind('mousewheel', function(e) {
              if (e.originalEvent.wheelDelta) {
                var scrollTop = resultContainer.scrollTop(),
                    scrollPos = scrollTop + resultContainer.innerHeight(),
                    scrollBottom = resultContainer[0].scrollHeight,
                    d = e.originalEvent.wheelDelta;

                if ((scrollPos === scrollBottom && d < 0) || (scrollTop === 0 && d > 0))
                  e.preventDefault();
              }
            });
          };

    map.add(markerLayer);
    map.add(shapeLayer);

    resultContainer.scroll(onScroll);

    btnFlag.click(onFlag);
    btnCancel.click(close);
    searchInput.keyup(function(e) {
      if (e.which === 13) {
        clear();
        currentSearch = searchInput.val().trim();
        if (currentSearch.length === 0)
          currentSearch = false;
        search();
      }
    });

    blockMouseWheelBubbling();

    this.open = open;
    HasEvents.apply(this);
  };
  GeoresolutionPanel.prototype = Object.create(HasEvents.prototype);

  return GeoresolutionPanel;

});
