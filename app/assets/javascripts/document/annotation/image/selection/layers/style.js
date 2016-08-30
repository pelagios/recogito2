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

    /** Rect & toponym styles **/
    BOX_BASELINE_WIDTH : 2,
    BOX_ANCHORDOT_RADIUS : 3,
    BOX_FILL_OPACITY : 0.2,
    BOX_STROKE_OPACITY : 0.55

  };

});
