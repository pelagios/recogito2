define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler',
  'document/annotation/image/selection/style'],

  function(Config, AbstractSelectionHandler, Style) {

    var PointSelectionHandler = function(containerEl, olMap, highlighter) {

      var self = this,

          pointVectorSource = new ol.source.Vector({}),

          currentSelection = false,

          isEnabled = false,

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
                mapBounds = pointToBounds(feature.getGeometry().getCoordinates());

            // Map bounds get centrally transformed to screenbounds by the MultiSelector - DRY
            currentSelection = {
              isNew      : false,
              annotation : annotation,
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

                mapBounds = pointToBounds(e.coordinate);

            pointVectorSource.clear(true);
            drawPoint(e.coordinate);

            // Map bounds get centrally transformed to screenbounds by the MultiSelector - DRY
            currentSelection = {
              isNew      : true,
              annotation : annotation,
              mapBounds  : mapBounds
            };

            self.fireEvent('select', currentSelection);
          },

          onClick = function(e) {
            var currentHighlight = highlighter.getCurrentHighlight();
            // TODO bit of a necessary intermediate hack to check if the highlight
            // TODO is a feature (not an annotation)
            if (currentHighlight && currentHighlight.getGeometry)
              // Select existing annotation
              selectExisting(currentHighlight);
            else if (isEnabled)
              // Create new selection if selector enabled
              selectNewPoint(e);
          },

          getSelection = function() {
            return currentSelection;
          },

          clearSelection = function(selection) {
            if (currentSelection.isNew)
              pointVectorSource.clear(true);

            currentSelection = false;
          },

          setEnabled = function(enabled) {
            isEnabled = enabled;
          };

      olMap.addLayer(new ol.layer.Vector({
        source: pointVectorSource,
        style: Style.POINT_HI
      }));
      olMap.on('click', onClick);

      this.getSelection = getSelection;
      this.clearSelection = clearSelection;
      this.setEnabled = setEnabled;

      AbstractSelectionHandler.apply(this);
    };
    PointSelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

    return PointSelectionHandler;

});
