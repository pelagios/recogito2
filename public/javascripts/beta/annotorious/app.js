var init = function(width, height) {
  var viewer = OpenSeadragon({
    id:'image-pane',
    prefixUrl: "https://cdn.jsdelivr.net/npm/openseadragon@2.4/build/openseadragon/images/",
    tileSources: [{
      type: 'zoomifytileservice',
      width: width,
      height: height,
      tilesUrl: '/document/' + config.documentId + '/part/' + config.partSequenceNo + '/tiles/'
    }]
  });

  var anno = OpenSeadragon.Annotorious(viewer);
};

(function() {
  fetch('/document/' + config.documentId + '/part/' + config.partSequenceNo + '/manifest')
    .then(response => response.text())
    .then(str => (new window.DOMParser()).parseFromString(str, "text/xml"))
    .then(data => {
      var props = data.firstChild,
          width = parseInt(props.getAttribute('WIDTH')),
          height = parseInt(props.getAttribute('HEIGHT'));

      init(width, height);
    });
})();