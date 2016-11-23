define(['common/config'], function(Config) {

  // Duration for showing the 'Save OK'/save error status info
  var SHOW_RESPONSE_STATUS_FOR_MS = 5000;

  var Header = function() {

    var annotationCountEl = jQuery('.quick-stats .annotations'),
        contributorsEl = jQuery('.quick-stats .contributors'),

        saveMessageEl = (function() {
          var el = jQuery('<div class="save-msg"></div>');
          jQuery('.header-iconbar').append(el);
          return el;
        })(),

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

            contributorsEl.html(updatedCount + label);
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

        showStatusSaved = function(message) {
          clearMessageFadeTimer();
          saveMessageEl.html('Annotation saved');
          saveMessageTimer = setTimeout(function() {
            saveMessageEl.fadeOut();
            saveMessageTimer = false;
          }, SHOW_RESPONSE_STATUS_FOR_MS);
        },

        showSaveError = function(error) {
          clearMessageFadeTimer();
          saveMessageEl.html('<span class="save-error">Error saving annotation. Try refreshing the page.</span>');
        };

    this.incrementAnnotationCount = incrementAnnotationCount;
    this.updateContributorInfo = updateContributorInfo;
    this.showStatusSaving = showStatusSaving;
    this.showStatusSaved = showStatusSaved;
    this.showSaveError = showSaveError;
  };

  return Header;

});
