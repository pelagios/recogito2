define([
  '../../../common/api',
  'georesolution/placeResult'], function(API, Result) {

  var GeoResolutionEditor = function() {
    var element = (function() {
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

            jQuery(document.body).append(el);
            return el;
          })(),

          searchInput = element.find('.search'),

          btnCancel = element.find('.cancel'),

          resultsHeader = element.find('.results-header'),

          resultsList = element.find('.results-list'),

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
                if (coord)
                  L.marker(coord).addTo(map);
              });
            });
          },

          open = function(quote, placeBody) {
            searchInput.val(quote);
            element.show();
            map.invalidateSize();
            search(quote);
          },

          close = function() {
            element.hide();
          };

    btnCancel.click(close);
    searchInput.keyup(function(e) { if (e.which === 13) onSearch(); });

    this.open = open;
  };

  return GeoResolutionEditor;

});
