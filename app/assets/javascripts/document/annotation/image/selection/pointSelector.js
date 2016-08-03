define(['common/hasEvents'], function(HasEvents) {

  var CLICK_DURATION_THRESHOLD_MS = 100,

      POINT_LAYER_STYLE = new ol.style.Style({
        image: new ol.style.Circle({
          radius : 8,
          fill   : new ol.style.Fill({ color: '#ff0000' }),
          stroke : new ol.style.Stroke({ color: '#bada55', width: 1 })
        })
      });

  var PointSelector = function(olMap) {
    var self = this,

        pointVectorSource = new ol.source.Vector({}),

        drawPoint = function(xy) {
          var pointFeature = new ol.Feature({
            'geometry': new ol.geom.Point(xy)
          });

          // pointVectorSource.addFeature(pointFeature);
        },

        onClick = function(e) {
          drawPoint(e.coordinate);
          self.fireEvent('select');
        };

    olMap.addLayer(new ol.layer.Vector({
      source: pointVectorSource,
      style: POINT_LAYER_STYLE
    }));

    olMap.on('click', onClick);

    HasEvents.apply(this);
  };
  PointSelector.prototype = Object.create(HasEvents.prototype);

  return PointSelector;

});
