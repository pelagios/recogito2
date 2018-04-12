define([
  'document/annotation/image/selection/layers/layer'
], function(Layer) {

  /** Constants **/
  var MIN_SELECTION_DISTANCE = 5;

  /** TODO make robust against change in argument order **/
  var parseAnchor = function(anchor) {
        var args = anchor.substring(anchor.indexOf(':') + 1).split(','),
            x = parseInt(args[0].substring(2)),
            y = parseInt(args[1].substring(2)),
            w = parseInt(args[2].substring(2)),
            h = parseInt(args[3].substring(2));

        return {
          x: x,  y: y, w: w, h: h,
          bounds : { top: - y, right: x + w, bottom: - y - h, left: x },
          coords : [
            [x, - y],
            [x, - y - h],
            [x + w, - y - h],
            [x + w, - y],
            [x, - y]
          ]
        };
      };

  var RectLayer = function(olMap) {
    Layer.apply(this, [ olMap ]);

    var self = this,

        rectVectorSource = new ol.source.Vector({}),

        buildStyle = function() {
          // Cf. http://stackoverflow.com/questions/28004153/setting-vector-feature-fill-opacity-when-you-have-a-hexadecimal-color
          var color = ol.color.asArray(self.getColorHex()),
              stroke = color.slice(),
              fill = color.slice();

          stroke[3] = self.getStrokeOpacity();
          fill[3] = self.getFillOpacity();

          return new ol.style.Style({
            stroke: new ol.style.Stroke({
              color: stroke,
              width: window.devicePixelRatio
            }),

            fill: new ol.style.Fill({
              color: fill
            })
          });
        },

        rectVectorLayer = new ol.layer.Vector({
          source: rectVectorSource,
          style: buildStyle()
        }),

        computeSize = function(annotation) {
          var parsed = parseAnchor(annotation.anchor);
          return parsed.w * parsed.h;
        },

        getAnnotationAt = function(e) {
          var hoveredFeatures = rectVectorSource.getFeaturesAtCoordinate(e.coordinate);

          if (hoveredFeatures.length > 0) {
            hoveredFeatures.sort(function(a, b) {
              var annotationA = a.get('annotation'),
                  annotationB = b.get('annotation');

              return computeSize(annotationA) - computeSize(annotationB);
            });

            return {
              annotation: hoveredFeatures[0].get('annotation'),
              imageBounds: hoveredFeatures[0].get('imageBounds')
            };
          }
        },

        findById = function(id) {
          var feature = self.findFeatureByAnnotationId(id, rectVectorSource);
          if (feature)
            return {
              annotation: feature.get('annotation'),
              imageBounds: feature.get('imageBounds')
            };
        },

        addAnnotation = function(annotation) {
          var parsed = parseAnchor(annotation.anchor),
              feature = new ol.Feature({
                'geometry': new ol.geom.Polygon([ parsed.coords ])
              });

          feature.set('annotation', annotation, true);
          feature.set('imageBounds', parsed.bounds, true);
          rectVectorSource.addFeature(feature);
        },

        redraw = function() {
          rectVectorLayer.setStyle(buildStyle());
        },

        removeAnnotation = function(annotation) {
          var feature = self.findFeatureByAnnotationId(annotation.annotation_id, rectVectorSource);
          if (feature)
            rectVectorSource.removeFeature(feature);
        },

        convertSelectionToAnnotation = function(selection) {
          addAnnotation(selection.annotation);
        },

        emphasiseAnnotation = function(annotation) {
          // TODO for future use
        },

        toggleVisibility = function() {
          rectVectorLayer.setVisible(!rectVectorLayer.getVisible());
        };

    olMap.addLayer(rectVectorLayer);

    this.computeSize = computeSize;
    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.redraw = redraw;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;
    this.toggleVisibility = toggleVisibility;
  };
  RectLayer.prototype = Object.create(Layer.prototype);

  return RectLayer;

});
