define([
  'common/ui/alert',
  'common/hasEvents'
], function(Alert, HasEvents) {

  var POLL_INTERVAL_MS = 1000;

  var ProgressModal = function(documentId) {
    var self = this,

        element = jQuery(
          '<div class="modal-clicktrap">' +
            '<div class="modal-wrapper">' +
              '<div class="modal progress">' +
                '<div class="modal-body">' +
                  '<p class="label">0%</p>' +
                  '<div class="meter">' +
                    '<div class="bar rounded" style="width:0"></div>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>'),

        label = element.find('.label'),

        bar = element.find('.bar'),

        updateProgress = function(response) {
          var p = response.progress + '%';
          label.html(p);
          bar.css('width', p);
        },

        queryProgress = function() {
          var retriesLeft = 3,

              onProgress = function(response) {
                var isStopped = response.status === 'COMPLETED' || response.status === 'FAILED';
                updateProgress(response);

                if (isStopped)
                  self.fireEvent('stopped', response);
                else
                  window.setTimeout(queryProgress, POLL_INTERVAL_MS);
              },

              onFail = function(error) {
                var isNotFound = error.status === 404;
                if (isNotFound && retriesLeft > 0) {
                  retriesLeft -= 1;
                  window.setTimeout(query, POLL_INTERVAL_MS);
                } else {
                  new Alert(Alert.ERROR, 'Error', error.responseText);
                }
              },

              query = function() {
                jsRoutes.controllers.api.TaskAPIController.progressByDocument(documentId).ajax()
                  .done(onProgress)
                  .fail(onFail);
              };

          query();
        },

        open = function() {
          jQuery(document.body).append(element);
          element.find('.modal-wrapper').draggable({ handle: '.modal-header' });
          queryProgress();
        },

        destroy = function() {
          element.remove();
        };

    this.open = open;
    this.destroy = destroy;

    HasEvents.apply(this);
  };
  ProgressModal.prototype = Object.create(HasEvents.prototype);

  return ProgressModal;

});
