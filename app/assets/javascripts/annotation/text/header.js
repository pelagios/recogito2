define(['../../common/config'], function(Config) {

  var Header = function() {
    var annotationCountEl = jQuery('.quick-stats .annotations'),
        contributorsEl = jQuery('.quick-stats .contributors'),

        saveMessageEl = jQuery('.save-msg'),
        saveMessageTimer = false,

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
        },

        clearMessageFadeTimer = function() {
          if (saveMessageTimer) {
            clearTimeout(saveMessageTimer);
            saveMessageTimer = false;
          }
          saveMessageEl.show();
        },

        showStatusSaving = function() {
          clearMessageFadeTimer();
          saveMessageEl.html('Saving...');
        },

        showStatusSaved = function() {
          clearMessageFadeTimer();
          saveMessageEl.html('All edits saved');
          saveMessageTimer = setTimeout(function() {
            saveMessageEl.fadeOut();
            saveMessageTimer = false;
          }, 5000);
        },

        showSaveError = function(error) {
          // TODO implement
        };

    this.incrementAnnotationCount = incrementAnnotationCount;
    this.updateContributorInfo = updateContributorInfo;
    this.showStatusSaving = showStatusSaving;
    this.showStatusSaved = showStatusSaved;
    this.showSaveError = showSaveError;
  };

  return Header;

});
