define([
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Layer, Style) {

  /** Constants **/
  var MIN_SELECTION_DISTANCE = 5;

  /** TODO make robust against change in argument order **/
  var parseAnchor = function(anchor) {
        var args = anchor.substring(anchor.indexOf(':') + 1).split(','),
            x = parseInt(args[0].substring(2)),
            y = parseInt(args[1].substring(2)),
            w = parseInt(args[2].substring(2)),
            h = parseInt(args[3].substring(2));

        return  {
          x: x,  y: y, w: w, h: h,
          bounds : { top: y, right: x + w, bottom: y + h, left: x },
          coords : [
            [x, - y],
            [x, - (y + h)],
            [x + w, - (y + h)],
            [x + w, - y],
            [x, - y]
          ]
        };
      };

  var RectLayer = function(olMap) {

    var self = this,

        rectVectorSource = new ol.source.Vector({}),

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
              origBounds: hoveredFeatures[0].get('origBounds')
            };
          }
        },

        findById = function(id) {
          var feature = self.findFeatureByAnnotationId(id, rectVectorSource);
          if (feature)
            return {
              annotation: feature.get('annotation'),
              origBounds: feature.get('origBounds')
            };
        },

        addAnnotation = function(annotation) {
          var parsed = parseAnchor(annotation.anchor),
              feature = new ol.Feature({
                'geometry': new ol.geom.Polygon([ parsed.coords ])
              });

          feature.set('annotation', annotation, true);
          feature.set('origBounds', parsed.bounds, true);
          rectVectorSource.addFeature(feature);
        },

        render = function() {
          // Do nothing - the rectangle layer renders immediately in addAnnotation
        },

        refreshAnnotation = function(annotation) {
          var existing = findById(annotation.annotation_id);
          if (existing) {
            // TODO doesn't happen yet
          } else {
            addAnnotation(annotation);
          }
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
          // TODO style change?
        };

    olMap.addLayer(new ol.layer.Vector({
      source: rectVectorSource,
      style: Style.BOX
    }));

    this.computeSize = computeSize;
    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.render = render;
    this.refreshAnnotation = refreshAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;

    Layer.apply(this, [ olMap ]);
  };
  RectLayer.prototype = Object.create(Layer.prototype);

  return RectLayer;

});
