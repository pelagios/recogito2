/**
 * A base class for the sections. Not strictly necessary, but gives
 * a proper name to things, and checks if the required 'abstract'
 * methods/properties needed for sections are implemented.
 */
define(['common/hasEvents'], function(HasEvents) {

  var Section = function() {

    if (!this.hasChanged)
      throw 'Section needs to implement .hasChanged() method';

    if (!this.commit)
      throw 'Section needs to implement .commit() method';

    if (!this.destroy)
      throw 'Section needs to implement .destroy() method';

    if (!this.body)
      throw 'Section needs to provide access to annotation body via .body property';

    HasEvents.apply(this);
  };
  Section.prototype = Object.create(HasEvents.prototype);

  return Section;

});
