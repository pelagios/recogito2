define([
  'common/ui/modal'
], function(Modal) {

  var QUERY_INTERVAL_MS = 1000;

  var ProgressModal = function(documentId) {
    var self = this,

        body = jQuery(
          '<div>' +
            '<p class="label">0%</p>' +
            '<div class="meter">' +
              '<div class="bar rounded" style="width:0"></div>' +
            '</div>' +
          '</div>'),

        label = body.find('.label'),
        bar = body.find('.bar'),

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
                  window.setTimeout(queryProgress, QUERY_INTERVAL_MS);
              },

              onFail = function(error) {
                var isNotFound = error.status === 404;
                if (isNotFound && retriesLeft > 0) {
                  retriesLeft -= 1;
                  window.setTimeout(query, QUERY_INTERVAL_MS);
                } else {
                  // TODO error popup
                  console.log(error);
                }
              },

              query = function() {
                jsRoutes.controllers.api.TaskAPIController.progressByDocument(documentId).ajax()
                  .done(onProgress)
                  .fail(onFail);
              };

          query();
        };

    this.queryProgress = queryProgress;

    Modal.apply(this, [ 'Progress', body, 'progress' ]);
  };
  ProgressModal.prototype = Object.create(Modal.prototype);

  /** Extends the open method provided by base Modal **/
  ProgressModal.prototype.open = function() {
    Modal.prototype.open.call(this);
    this.queryProgress();
  };

  return ProgressModal;

});
