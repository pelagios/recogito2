require(['../../common/config'], function(Config) {

  var POLL_INTERVAL = 1000, // Poll interval in milliseconds

      // Map (task type -> query URL)
      QUERY_URLS = {
        NER          : '/' + Config.owner + '/upload/ner-progress/' + Config.documentId,
        IMAGE_TILING : '/' + Config.owner + '/upload/tiling-progress/' + Config.documentId
      },

      // Map listing the tasks that apply to each content type
      APPLICABLE_TASKS = {
        TEXT_PLAIN : [ 'NER' ],
        IMAGE_UPLOAD: [ 'IMAGE_TILING' ]
      },

      Filepart = function(containerElement, runningTasks) {
                   var element = jQuery(containerElement).find('.filepart-processing-progress'),

                       contentType = jQuery(containerElement).data('type'),

                       id = jQuery(containerElement).data('id'),

                       // The running tasks that apply to this particular contentType
                       applicableRunningTasks = (function() {
                         var applicableTasks = APPLICABLE_TASKS[contentType];

                         return jQuery.grep(runningTasks, function(runningTask) {
                           return applicableTasks.indexOf(runningTask) > -1;
                         });
                       })(),

                       progressPerTask = (function() {
                         var progressPerTask = {};

                         jQuery.each(applicableRunningTasks, function(idx, t) {
                           progressPerTask[t] = 'PENDING';
                         });

                         return progressPerTask;
                       })(),

                       updateElement = function(progress) {
                         if (isSuccess()) {
                           element.addClass('completed');
                           element.html('&#xf00c');
                         } else  if (isFailed()) {
                           element.addClass('failed');
                           element.html('&#xf00d');
                         }
                       },

                       isPending = function() {
                         var completedTasks = [];

                         jQuery.each(progressPerTask, function(task, status) {
                           if (status === 'COMPLETED' || status === 'FAILED')
                             completedTasks.push(task);
                         });

                         return completedTasks.length !== applicableRunningTasks.length;
                       },

                       /** Returns true if ALL tasks on this part are COMPLETED **/
                       isSuccess = function() {
                         var succeededTasks = [];

                         jQuery.each(progressPerTask, function(task, status) {
                           if (status === 'COMPLETED')
                             succeededTasks.push(task);
                         });

                         return succeededTasks.length === applicableRunningTasks.length;
                       },

                       /** Returns true if ANY task on this part is FAILED **/
                       isFailed = function() {
                         var failedTasks = [];

                         jQuery.each(progressPerTask, function(task, status) {
                           if (status === 'FAILED')
                             failedTasks.push(task);
                         });

                         return failedTasks.length > 0;
                       };

                   // Updates the filepart element with the given progress information
                   this.update = function(progress) {
                     if (applicableRunningTasks.indexOf(progress.task_name) > -1) {
                       var progressOnThisPart = jQuery.grep(progress.progress, function(p) {
                                                  return p.filepart_id === id;
                                                })[0];

                       progressPerTask[progress.task_name] = progressOnThisPart.status;
                       updateElement(progress);
                     }
                   };

                   // Returns true if this filepart is still being processed
                   this.isPending = isPending;
                 };

  jQuery(document).ready(function() {

    var fileparts = jQuery.map(jQuery('.filepart-preview'), function(el) {
          return new Filepart(el, Config.tasks);
        }),

        /** Returns true if all parts are either completed or failed **/
        allPartsFinished = function() {
          var finishedParts = jQuery.grep(fileparts, function(part) {
            return !part.isPending();
          });

          return finishedParts.length === fileparts.length;
        },

        // Polls the progress on the processing task with the specified URL
        pollProcessingProgress = function(taskType) {
          var queryURL = QUERY_URLS[taskType];

          jQuery.getJSON(queryURL, function(response) {
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
              setTimeout(function() { pollProcessingProgress(taskType); }, POLL_INTERVAL);

            // Check if we're done
            if (allPartsFinished())
              jQuery('.btn.next').prop('disabled', false);
          });
        };

    // Start polling task progress
    jQuery.each(Config.tasks, function(idx, taskURL) {
      pollProcessingProgress(taskURL);
    });

    // Make filepart elements sortable (and disable selection)
    jQuery('ul.fileparts').sortable();
    jQuery('ul.fileparts').disableSelection();
  });

});
