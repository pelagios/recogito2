define(['../../../../common/formatting', '../../../../common/placeUtils'], function(Formatting, PlaceUtils) {

  var PlaceSection = function(parent) {
    var element = (function() {
          var el = jQuery(
            '<div class="section place">' +
              '<div class="map"></div>' +
              '<div class="panel-container">' +
                '<div class="panel">' +
                  '<h3></h3>' +
                  '<p class="gazetteer"></p>' +
                  '<p class="description"></p>' +
                  '<p class="names"></p>' +
                  '<p class="date"></p>' +
                  // '<div class="created">' +
                  //   '<a class="by" href="#">rainer</a>' +
                  //   '<span class="at">2 days ago</span>' +
                  // '</div>' +
                  // '<a href="#" class="btn tiny change">Change</a>' +
                  '<div class="warning-unverified">' +
                    '<span class="warning"><span class="icon">&#xf071;</span> Automatic Match</span>' +
                    '<button>Change</button>' +
                    '<button>Confirm</button>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>');

          parent.prepend(el);
          return el;
        })(),

        title = element.find('h3'),
        gazetteerId = element.find('.gazetteer'),
        date = element.find('.date'),
        names = element.find('.names'),
        description = element.find('.description'),

        awmc = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
          attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                       'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                       'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                       '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
        }),

        map = L.map(element.find('.map')[0], {
          center: new L.LatLng(41.893588, 12.488022),
          zoom: 5,
          zoomControl: false,
          layers: [ awmc ]
        }),

        setCenter = function(latLon) {
          var centerOnLayer = map.latLngToContainerPoint(latLon);
          centerOnLayer = centerOnLayer.subtract([290, 10]);
          map.setView(map.layerPointToLatLng(centerOnLayer), 4, { animate: false });
        },

        fillTemplate = function(gazetteerRecord, labels, latLon) {
          // Mandatory fields
          title.html(gazetteerRecord.title);
          gazetteerId.html(Formatting.formatGazetteerURI(gazetteerRecord.uri));
          names.html(labels.slice(1).join(', '));

          // Optional fields
          if (gazetteerRecord.temporal_bounds) {
            date.html(Formatting.yyyyMMddToYear(gazetteerRecord.temporal_bounds.from) + ' - ' +
                      Formatting.yyyyMMddToYear(gazetteerRecord.temporal_bounds.to));
            date.show();
          } else {
            date.empty();
            date.hide();
          }

          if (gazetteerRecord.descriptions) {
            description.html(gazetteerRecord.descriptions[0].description);
            description.show();
          } else {
            description.empty();
            description.hide();
          }

          // Map
          L.marker(latLon).addTo(map);
          setCenter(latLon);
        },

        automatch = function(str) {
          jQuery.getJSON('/api/places/search?q=' + str, function(response) {
            var topPlace = response.items[0],
                bestRecord = PlaceUtils.getBestMatchingRecord(topPlace),
                coord = bestRecord.representative_point;

            fillTemplate(bestRecord, topPlace.labels, [ coord[1], coord[0] ]);
          });
        },

        destroy = function() {
          element.remove();
        };

    this.automatch = automatch;
    this.destroy = destroy;
  };

  return PlaceSection;

});
