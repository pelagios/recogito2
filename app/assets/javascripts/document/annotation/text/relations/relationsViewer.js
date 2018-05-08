define(['document/annotation/text/relations/relation'], function(Connection) {

  var RelationsViewer = function(content, svg) {

    var getNode = function(annotationId) {
          var elements = jQuery('*[data-id="' + annotationId + '"]'),
              annotation;

          if (elements.length > 0)
            return {
              annotation: elements[0].annotation,
              elements: elements
            };
        },

        init = function(annotations) {
          var relations = annotations.reduce(function(arr, annotation) {
            if (annotation.relations && annotation.relations.length > 0) {

              var connections = annotation.relations.map(function(r) {
                var fromNode = getNode(annotation.annotation_id),
                    toNode = getNode(r.relates_to);
                return new Connection(svg, fromNode, toNode);
              });

              connections.forEach(function(c) { c.redraw(); });

              return arr.concat(annotation.relations);
            } else {
              return arr;
            }
          }, []);
        };


    this.init = init;
  };

  return RelationsViewer;

});
