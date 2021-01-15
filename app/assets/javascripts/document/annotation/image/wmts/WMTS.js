/** 
 * Creates the union bounding box from the given array
 * of individual layer bounding boxes.
 */
var unionBBox = function(bounds) {
  // TODO implement!
  return bounds[0]; // Temporary hack for testing
}

define([], function() { return {

  buildTileSourceConfig: function(capabilitiesResponse) {
    var parser = new ol.format.WMTSCapabilities(),

        capabilities = parser.read(capabilitiesResponse),

        layers = capabilities.Contents.Layer.map(function(layer) {
          return layer.Identifier;
        }),

        bbox = unionBBox(capabilities.Contents.Layer.map(function(layer) {
          return layer.WGS84BoundingBox;
        })),

        center = ol.proj.transform([
          (bbox[0] + bbox[2]) / 2,
          (bbox[1] + bbox[3]) / 2
        ], 'EPSG:4326', 'EPSG:3857'),

        bottomLeft = ol.proj.transform([ bbox[0], bbox[1] ], 'EPSG:4326', 'EPSG:3857'),

        topRight = ol.proj.transform([ bbox[2], bbox[3] ], 'EPSG:4326', 'EPSG:3857'),

        extent = [ bottomLeft[0], bottomLeft[1], topRight[0], topRight[1] ],

        tileSources = layers.map(function(layer) {
          var options = ol.source.WMTS.optionsFromCapabilities(capabilities, {
            layer: layer,
            projection: ol.proj.get('EPSG:3857')
          });

          return new ol.source.WMTS(options);
        });

    return {
      tileSources: tileSources,
      center: center,
      extent: extent
    } 
  }

}});