/** Drawing style constants **/
define([], function() {

  return {

    /** Colors **/
    COLOR_GREY : '#323232',

    /** Openlayers-ready point styles **/
    POINT : new ol.style.Style({
      image: new ol.style.Circle({
        radius : 4,
        fill   : new ol.style.Fill({ color: [ 68, 131, 196, 1 ] }),
        stroke : new ol.style.Stroke({ color: '#1d5b9b', width: 1.5 })
      })
    }),

    POINT_HI : new ol.style.Style({
      image: new ol.style.Circle({
        radius : 8,
        fill   : new ol.style.Fill({ color: [ 68, 131, 196, 1 ] }),
        stroke : new ol.style.Stroke({ color: '#1d5b9b', width: 1.5  })
      })
    }),

    /** Rect & box styles **/
    BOX_BASELINE_WIDTH : 2,
    BOX_ANCHORDOT_RADIUS : 3,
    BOX_FILL_OPACITY : 0.2,

    /** OpenLayers-ready box style **/
    BOX : (function() {

      // Cf. http://stackoverflow.com/questions/28004153/setting-vector-feature-fill-opacity-when-you-have-a-hexadecimal-color
      var baseGrey = ol.color.asArray('#323232'),
          strokeGrey = baseGrey.slice(),
          fillGrey = baseGrey.slice();

      strokeGrey[3] = 0.55;
      fillGrey[3] = 0.2;

      return new ol.style.Style({
        stroke: new ol.style.Stroke({
          color: strokeGrey,
          width: 1
        }),

        fill: new ol.style.Fill({
          color: fillGrey
        })
      });
    })()

  };

});
