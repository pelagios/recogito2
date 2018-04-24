require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/config'], function(Config) {

  var POLL_INTERVAL = 1000, // Poll interval in milliseconds

      // Map (task type -> query URL)
      QUERY_URL = '/' + Config.owner + '/upload/progress/' + Config.documentId,

      Filepart = function(containerElement) {
        var element = jQuery(containerElement).find('.filepart-processing-progress'),

            contentType = jQuery(containerElement).data('type'),

            id = jQuery(containerElement).data('id'),

            runningTasks = [],

            updateElement = function() {
              if (isSuccess()) {
                element.addClass('completed');
                element.html('&#xf00c');
              } else  if (isFailed()) {
                element.addClass('failed');
                element.html('&#xf00d');
              }
            },

            isPending = function() {
              var completedTasks = jQuery.grep(runningTasks, function(task) {
                return task.status === 'COMPLETED' || task.status === 'FAILED';
              });

              return completedTasks.length !== runningTasks.length;
            },

            /** Returns true if all running tasks on this part are COMPLETED **/
            isSuccess = function() {
              var succeededTasks = jQuery.grep(runningTasks, function(task) {
                return task.status === 'COMPLETED';
              });

              return runningTasks.length > 0 && succeededTasks.length === runningTasks.length;
            },

            /** Returns true if ANY task on this part is FAILED **/
            isFailed = function() {
              var failedTasks = jQuery.grep(runningTasks, function(task) {
                return task.status === 'FAILED';
              });

              return failedTasks.length > 0;
            };

        /** Returns the current sort index in the list **/
        this.getSortPosition = function() {
          return jQuery(containerElement).index();
        };

        /** Updates the filepart element with the given progress information **/
        this.update = function(progress) {
          runningTasks = jQuery.grep(progress.subtasks, function(subtask) {
            return subtask.filepart_id === id;
          });

          updateElement();
        };

        this.id = id;

        // Returns true if this filepart is still being processed
        this.isPending = isPending;
      };

  jQuery(document).ready(function() {

    var fileparts = jQuery.map(jQuery('.filepart-preview'), function(el) {
          return new Filepart(el);
        }),

        /** Returns true if all parts are either completed or failed **/
        allPartsFinished = function() {
          var finishedParts = jQuery.grep(fileparts, function(part) {
            return !part.isPending();
          });

          return finishedParts.length === fileparts.length;
        },

        // Polls the progress on the processing task with the specified URL
        pollProcessingProgress = function(queryURL) {
          jQuery.getJSON(QUERY_URL, function(response) {
            var pendingWorkers;

            // Update all fileparts
            jQuery.each(fileparts, function(idx, part) {
              part.update(response);
            });

            // Check if any filepart is waiting for more updates of this task type
            pendingWorkers = jQuery.grep(fileparts, function(part) {
              return part.isPending();
            });

            if (pendingWorkers.length > 0)
              setTimeout(function() { pollProcessingProgress(); }, POLL_INTERVAL);

            // Check if we're done
            if (allPartsFinished())
              jQuery('.btn.next').prop('disabled', false);
          });
        },

        onOrderChanged = function() {
          var sortOrder = jQuery.map(fileparts, function(part) {
            return { id: part.id, sequence_no: part.getSortPosition() + 1 };
          });

          jsRoutes.controllers.document.settings.SettingsController.setSortOrder(Config.documentId).ajax({
            data: JSON.stringify(sortOrder),
            contentType: 'application/json'
          }).fail(function(error) {
            // TODO present error to user
            console.log(error);
          });
        };

    // Start polling task progress
    if (Config.tasks.length === 0)
      jQuery('.btn.next').prop('disabled', false);
    else
      pollProcessingProgress();

    // Make filepart elements sortable (and disable selection)
    jQuery('ul.fileparts').disableSelection();
    jQuery('ul.fileparts').sortable({
      stop: onOrderChanged
    });
  });

});
