define(['../../../common/placeUtils'], function(PlaceUtils) {

  var PlaceSection = function(parent) {
    var element = (function() {
          var el = jQuery(
            '<div class="section place">' +
              '<div class="map"></div>' +
              '<div class="panel-container">' +
                '<div class="panel">' +
                  '<div class="warning-unverified">' +
                    '<span class="icon">&#xf071;</span> Automatic Match ' +
                    '<div class="buttons">' +
                      '<button class="link">Confirm</button>' +
                      '<button class="link">Change</button>' +
                    '</div>' +
                  '</div>' +
                  '<h3></h3>' +
                  '<p class="names"></p>' +
                '</div>' +
              '</div>' +
            '</div>');

          parent.append(el);
          return el;
        })(),

        titleEl = element.find('h3'),

        namesEl = element.find('p.names'),

        awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = new L.Map(element.find('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 5,
          zoomControl: false,
          layers: [ awmc ]
        }),

        setCenter = function(lat, lon) {
          var centerOnLayer = map.latLngToContainerPoint([lat, lon]);
          centerOnLayer = centerOnLayer.subtract([320, 10]);
          map.setView(map.layerPointToLatLng(centerOnLayer), 4, { animate: false });
        },

        fillWithDummyContent = function(selectedText) {
          jQuery.getJSON('/api/places/search?q=' + selectedText, function(response) {
            var topPlace = response.items[0],
                pt = topPlace.representative_point,
                bestRecord = PlaceUtils.getBestMatchingRecord(topPlace),
                labels = PlaceUtils.getLabels(bestRecord);

            titleEl.html(labels[0]);
            namesEl.html(labels.slice(1).join(', '));

            L.marker([pt[1], pt[0]]).addTo(map);
            setCenter(pt[1], pt[0]);
          });
        };

    fillWithDummyContent('rome');
  };

  return PlaceSection;

});
