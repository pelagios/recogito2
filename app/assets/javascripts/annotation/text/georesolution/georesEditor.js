define([
  '../../../common/helpers/formatting',
  '../../../common/helpers/placeUtils',
  '../../../common/api',
  '../../../common/hasEvents',
  'georesolution/placeResult'], function(Formatting, PlaceUtils, API, HasEvents, Result) {

  var GeoResolutionEditor = function() {
    var self = this,

        element = (function() {
            var el = jQuery('<div class="clicktrap">' +
                  '<div class="geores-wrapper">' +
                    '<div class="geores-editor">' +
                      '<div class="geores-editor-header">' +
                        '<div class="geores-search">' +
                          '<input class="search inline" type="text" placeholder="Search for a Place..." />' +
                          '<button class="search icon">&#xf002;</button>' +
                        '</div>' +
                        '<div class="flag-this-place">' +
                          'Can\'t find the right match?' +
                          '<span class="flag">' +
                            '<button class="nostyle">&#xe842;</button>' +
                            '<span class="label">Flag this place</span>' +
                          '</span>' +
                        '</div>' +
                        '<button class="nostyle cancel">&#xe897;</button>' +
                      '</div>' +
                      '<div class="geores-editor-body">' +
                        '<div class="geores-sidebar">' +
                          '<div class="results-header"></div>' +
                          '<ul class="results-list"></ul>' +
                        '</div>' +
                        '<div class="geores-map">' +
                        '</div>' +
                      '</div>' +
                    '</div>' +
                  '</div>' +
                '</div>');

            el.hide();
            jQuery(document.body).append(el);
            return el;
          })(),

          searchInput = element.find('.search'),

          btnCancel = element.find('.cancel'),

          resultsHeader = element.find('.results-header'),

          resultsList = element.find('.results-list'),

          currentBody = false,

          awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
            attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                         'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                         'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                         '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
          }),

          map = L.map(element.find('.geores-map')[0], {
            center: new L.LatLng(41.893588, 12.488022),
            zoom: 4,
            zoomControl: false,
            layers: [ awmc ]
          }),

          openPopup = function(marker, place) {
            var popup = jQuery(
                  '<div class="popup">' +
                    '<div class="popup-header">' +
                      '<h3>' + place.labels.join(', ') + '</h3>' +
                    '</div>' +
                    '<div class="popup-details">' +
                      '<p class="description"></p>' +
                      '<p class="date"></p>' +
                    '</div>' +
                    '<div class="popup-choices">' +
                      '<p>Select Gazetteer</p>' +
                      '<table></table>' +
                    '</div>' +
                  '</div>'),

                descriptions =  PlaceUtils.getDescriptions(place);

            // Details
            if (descriptions.length > 0)
              popup.find('.description').html(descriptions[0].description);
            else
              popup.find('.description').hide();

            if (place.temporal_bounds)
              popup.find('.date').html(
                Formatting.yyyyMMddToYear(place.temporal_bounds.from) + ' - ' +
                Formatting.yyyyMMddToYear(place.temporal_bounds.to));
            else
              popup.find('.date').hide();

            // Gazetteer choices
            jQuery.each(place.is_conflation_of, function(idx, choice) {
              popup.find('.popup-choices table').append(
                '<tr data-uri="' + choice.uri + '">' +
                  '<td>' + Formatting.formatGazetteerURI(choice.uri) + '</td>' +
                  '<td class="select">SELECT</td>' +
                '</tr>');
            });

            popup.find('table').on('click', 'tr', function(e) {
              var tr = jQuery(e.target).closest('tr');
              self.fireEvent('update', currentBody, tr.data('uri'));
              close();
            });

            marker.bindPopup(popup[0]).openPopup();
          },

          onSearch = function() {
            var query = searchInput.val().trim();
            if (query.length > 0)
              search(query);
          },

          search = function(query) {
            resultsList.empty();
            API.searchPlaces(query).done(function(response) {
              // TODO dummy only
              resultsHeader.html("Total: " + response.total + ", took " + response.took);
              jQuery.each(response.items, function(idx, place) {
                var coord = (place.representative_point) ?
                      [ place.representative_point[1], place.representative_point[0] ] :
                      false;

                new Result(resultsList, place);
                if (coord) {
                  L.marker(coord).addTo(map).on('click', function(e) {
                    openPopup(e.target, place);
                  });
                }
              });
            });
          },

          open = function(quote, placeBody) {
            currentBody= placeBody;

            searchInput.val(quote);
            element.show();
            map.invalidateSize();
            search(quote);
          },

          close = function() {
            currentBody = false;
            element.hide();
          };

    btnCancel.click(close);
    searchInput.keyup(function(e) { if (e.which === 13) onSearch(); });

    this.open = open;
    HasEvents.apply(this);
  };
  GeoResolutionEditor.prototype = Object.create(HasEvents.prototype);


  return GeoResolutionEditor;

});
