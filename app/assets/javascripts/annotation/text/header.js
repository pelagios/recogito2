define(['../../common/config'], function(Config) {

  var Header = function(parentEl) {
        /** DOM elements holding annotation and contributor information **/
    var annotationCountEl = parentEl.find('.quick-stats .annotations'),
        contributorsEl = parentEl.find('.quick-stats .contributors'),

        /** Document owner username **/
        owner = Config.documentOwner,

        /** List of contributors **/
        knownContributors = [],

        /**
         * Increments the annotation count by the specified value (or one, if omitted)
         * and returns the new annotation count for convenience.
         */
        incrementAnnotationCount = function(incrementBy) {
          var inc = (incrementBy !== undefined) ? incrementBy : 1,
              currentCount = parseInt(annotationCountEl.text()),
              updatedCount = (currentCount) ? currentCount + inc : inc;

          annotationCountEl.html(updatedCount);
          return updatedCount;
        },

        /** Checks if this user is a new contributor **/
        isNewContributor = function(username) {
          // Owner not recorded as 'contributor' in the header
          if (username === owner)
            return false;
          else
            return knownContributors.indexOf(username) === -1;
        },

        updateContributorInfo = function(username) {
          var currentCount, updateCount;

          if (isNewContributor(username)) {
            knownContributors.push(username);
            currentCount = parseInt(contributorsEl.text());
            updatedCount = (currentCount) ? currentCount + 1 : 1;
            label = (updatedCount === 1) ? ' Other Contributor' : ' Other Contributors';

            contributorEl.html(updateCount + label);
          }
        };

    this.incrementAnnotationCount = incrementAnnotationCount;
    this.updateContributorInfo = updateContributorInfo;
  };

  return Header;

});
