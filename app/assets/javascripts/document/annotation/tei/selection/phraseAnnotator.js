/**
 * Productivity feature: queries for untagged occurences of a phrase, and automatically
 * prompts the user to re-apply the current annotation.
 */
define([
  'common/utils/annotationUtils',
  'common/config'
], function(AnnotationUtils, Config) {

  var PhraseAnnotator = function(contentEl, highlighter) {

    var countOccurrences = function(phrase) {

          /*
          getUnannotatedSegments().reduce(function(sum, textNode) {
            var split = textNode.nodeValue.split(phrase);
            return sum + split.length - 1;
          }, 0);
          */

          return 0;
        },

        createSelections = function(annotation) {

        };

    this.countOccurrences = countOccurrences;
    this.createSelections = createSelections;
  };

  return PhraseAnnotator;

 });
