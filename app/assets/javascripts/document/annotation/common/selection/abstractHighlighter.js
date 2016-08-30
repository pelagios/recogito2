/**
 * A base class for highlighter implementations on different media (text, image). Not
 * really necessary in dynamically-typed JavaScript-land, but gives a proper name to
 * things, and checks if the interface contract is (at least partially...) fulfilled.
 */
define([], function() {

  var AbstractHighlighter = function() {

    if (!this.findById)
      throw 'Highlighter needs to implement .findById() method';

    if (!this.initPage)
      throw 'Highlighter needs to implement .initPage() method';

    if (!this.refreshAnnotation)
      throw 'Highlighter needs to implement .refreshAnnotation() method';

    if (!this.removeAnnotation)
      throw 'Highlighter needs to implement .removeAnnotation() method';

  };

  return AbstractHighlighter;

});
