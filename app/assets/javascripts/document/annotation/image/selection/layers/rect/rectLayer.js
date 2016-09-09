define([
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style'
], function(Layer, Style) {

  var RectLayer = function(olMap) {

    var rectVectorSource = new ol.source.Vector({}),

        getAnnotationAt = function(e) {

        },

        findById = function(id) {

        },

        addAnnotation = function(annotation) {
          console.log(annotation);
        },

        render = function() {
          // Do nothing - the rectangle layer renders immediately in addAnnotation
        },

        refreshAnnotation = function(annotation) {
          // TODO style change depending on annotation properties
        },

        removeAnnotation = function(annotation) {

        },

        convertSelectionToAnnotation = function(selection) {

        },

        emphasiseAnnotation = function(annotation) {
          // TODO style change?
        };

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
