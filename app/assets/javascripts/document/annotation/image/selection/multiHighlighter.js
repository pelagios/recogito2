define([
  'common/config',
  'document/annotation/common/selection/abstractHighlighter',
  'document/annotation/image/selection/point/pointHighlighter',
  'document/annotation/image/selection/toponym/toponymHighlighter'],

  function(Config, AbstractHighlighter, PointHighlighter, ToponymHighlighter) {

    var MultiHighlighter = function(olMap) {

      var highlighters = {
            point : new PointHighlighter(olMap),
            toponym : new ToponymHighlighter(olMap)
          },

          /**
           * Calls the function with the given name on the appropriate highlighter, with
           * the annotation as function argument.
           */
          applyToAppropriateHighlighter = function(annotation, fnName) {
            var anchor = annotation.anchor,
                shapeType = anchor.substring(0, anchor.indexOf(':')).toLowerCase(),
                highlighter = highlighters[shapeType];

            if (highlighter)
              highlighter[fnName](annotation);
          },

          /** Calls the function on all highlighters, expecting 0 or one responses **/
          findFromAllHighlighters = function(fnName) {
            var results = [];

            jQuery.each(highlighters, function(key, highlighter) {
              var result = highlighter[fnName]();
              if (result)
                results.push(result);
            });

            if (results.length > 0)
              return results[0];
          },

          findById = function() {
            return findFromAllHighlighters('findById');
          },

          getCurrentHighlight = function() {
            return findFromAllHighlighters('getCurrentHighlight');
          },

          initPage = function(annotations) {
            // Add annotations to appropriate highlighter
            jQuery.each(annotations, function(idx, a) {
              applyToAppropriateHighlighter(a, 'addAnnotation');
            });

            // Render all highlighters
            jQuery.each(highlighters, function(key, highlighter) {
              highlighter.render();
            });
          },

          refreshAnnotation = function(annotation) {
            applyToAppropriateHighlighter(annotation, 'refreshAnnotation');
          },

          removeAnnotation = function() {
            applyToAppropriateHighlighter(annotation, 'removeAnnotation');
          },

          convertSelectionToAnnotation = function(selection, annotationStub) {
            // Text mode requires special treatment to convert a selection SPAN to
            // an annoation SPAN (change of CSS classes, adding data attribute etc.)
            // Image mode is much simpler - we just need to draw the annotation stub
            // and can ignore the selection
            applyToAppropriateHighlighter(annotationStub, 'renderAnnotation');
          };

      this.getCurrentHighlight = getCurrentHighlight;
      this.findById = findById;
      this.initPage = initPage;
      this.refreshAnnotation = refreshAnnotation;
      this.removeAnnotation = removeAnnotation;
      this.convertSelectionToAnnotation = convertSelectionToAnnotation;

      AbstractHighlighter.apply(this);
    };
    MultiHighlighter.prototype = Object.create(AbstractHighlighter.prototype);

    return MultiHighlighter;

});
