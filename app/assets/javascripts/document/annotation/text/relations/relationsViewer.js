define(['document/annotation/text/relations/relation'], function(Connection) {

  var RelationsViewer = function(content, svg) {

    var connections = [],

        getNode = function(annotationId) {
          var elements = jQuery('*[data-id="' + annotationId + '"]'),
              annotation;

          if (elements.length > 0)
            return {
              annotation: elements[0].annotation,
              elements: elements
            };
        },

        init = function(annotations) {
          connections = annotations.reduce(function(arr, annotation) {
            if (annotation.relations && annotation.relations.length > 0) {

              var connections = annotation.relations.map(function(r) {
                var fromNode = getNode(annotation.annotation_id),
                    toNode = getNode(r.relates_to);
                return new Connection(svg, fromNode, toNode);
              });

              connections.forEach(function(c) { c.redraw(); });
              return arr.concat(connections);
            } else {
              return arr;
            }
          }, []);
        },

        redrawAll = function() {
          connections.forEach(function(connection) {
            connection.recompute();
          });
        };

    jQuery(window).on('resize', redrawAll);

    this.init = init;
  };

  return RelationsViewer;

});
