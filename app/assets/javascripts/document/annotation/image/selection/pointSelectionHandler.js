define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'],

  function(Config, AbstractSelectionHandler) {

    var POINT_SELECTION_STYLE = new ol.style.Style({
          image: new ol.style.Circle({
            radius : 6,
            fill   : new ol.style.Fill({ color: '#4483C4', opacity: 0.35 }),
            stroke : new ol.style.Stroke({ color: '#366696', width: 1.5 })
          })
        });

    var PointSelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          pointVectorSource = new ol.source.Vector({}),

          currentSelection = false,

          /** Converts the given map-coordinate bounds to viewport bounds **/
          mapBoundsToScreenBounds = function(mapBounds) {
            var offset = jQuery(containerEl).offset(),
                topLeft = olMap.getPixelFromCoordinate([ mapBounds.left, mapBounds.top ]),
                bottomRight = olMap.getPixelFromCoordinate([ mapBounds.right, mapBounds.bottom ]);

            return {
              top    : topLeft[1] + offset.top,
              right  : bottomRight[0] + offset.left,
              bottom : bottomRight[1] + offset.top,
              left   : topLeft[0] + offset.left,
              width  : 0,
              height : 0
            };
          },

          pointToBounds = function(coordinate) {
            return {
              top    : coordinate[1],
              right  : coordinate[0],
              bottom : coordinate[1],
              left   : coordinate[0],
              width  : 0,
              height : 0
            };
          },

          /** Draws the point selection on the map **/
          drawPoint = function(coordinate) {
            var pointFeature = new ol.Feature({
                  'geometry': new ol.geom.Point(coordinate)
                });

            pointVectorSource.addFeature(pointFeature);
          },

          selectExisting = function(feature) {
            var annotation = feature.get('annotation'),
                mapBounds = pointToBounds(feature.getGeometry().getCoordinates()),
                screenBounds = mapBoundsToScreenBounds(mapBounds);

            currentSelection = {
              isNew      : false,
              annotation : annotation,
              bounds     : screenBounds,

              // Image-UI specific field - needed to provide up-to-date
              // screenbounds in .getSelection
              mapBounds  : mapBounds
            };

            self.fireEvent('select', currentSelection);
          },

          /** Compiles an annotation stub and draws the selected point **/
          selectNewPoint = function(e) {
            var x = Math.round(e.coordinate[0]),

                y = Math.abs(Math.round(e.coordinate[1])),

                annotation = {
                  annotates: {
                    document_id: Config.documentId,
                    filepart_id: Config.partId,
                    content_type: Config.contentType
                  },
                  anchor: 'point:' + x + ',' + y,
                  bodies: []
                },

                mapBounds = pointToBounds(e.coordinate),

                screenBounds = mapBoundsToScreenBounds(mapBounds);

            pointVectorSource.clear(true);
            drawPoint(e.coordinate);

            currentSelection = {
              isNew      : true,
              annotation : annotation,
              bounds     : screenBounds,

              // Image-UI specific field - needed to provide up-to-date
              // screenbounds in .getSelection
              mapBounds  : mapBounds
            };

            self.fireEvent('select', currentSelection);
          },

          onClick = function(e) {
            var currentHighlight = highlighter.getCurrentHighlight();
            if (currentHighlight)
              // Select existing annotation
              selectExisting(currentHighlight);
            else
              // Create new selection
              selectNewPoint(e);
          },

          getSelection = function() {
            // Update screenbounds, since the map may have moved
            if (currentSelection)
              currentSelection.bounds = mapBoundsToScreenBounds(currentSelection.mapBounds);
            return currentSelection;
          },

          clearSelection = function(selection) {
            if (currentSelection.isNew)
              pointVectorSource.clear(true);

            currentSelection = false;
          };

      olMap.addLayer(new ol.layer.Vector({
        source: pointVectorSource,
        style: POINT_SELECTION_STYLE
      }));
      olMap.on('click', onClick);

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;

      AbstractSelectionHandler.apply(this);
    };
    PointSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return PointSelectionHandler;

});
