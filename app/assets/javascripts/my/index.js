require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/alert',
  'common/ui/touch',
  'common/utils/urlUtils',
  'common/config'
], function(Alert, Touch, URLUtils, Config) {

  jQuery(document).ready(function() {
        /** Document elements **/
    var documents = jQuery('.document'),

        /** Table sort buttons **/
        sortButtons = jQuery('.sortable'),

        /** Tool buttons **/
        btnDeleteSelected = jQuery('button.delete'),
        btnCreateFolder = jQuery('button.add-folder'),

        /** Search **/
        searchbox = jQuery('input.search'),

        /** TODO these will be normal links later **/
        btnAccountSettings = jQuery('.account-settings'),
        btnGridView = jQuery('.display-mode'),

        formatTime = function() {
          jQuery('time.timeago').timeago();
        },

        /** Resolves the click target to the parent document element **/
        getClickedDocument = function(e) {
          var docEl = jQuery(e.target).closest('.document');
          if (docEl.length > 0)
            return docEl;
          else
            return false;
        },

        /** Returns the IDs of the currently selected documents **/
        getSelectedDocumentIDs = function() {
          var selected = jQuery.grep(documents, function(docEl) {
            return jQuery(docEl).hasClass('selected');
          });

          return jQuery.map(selected, function(el) {
            return el.dataset.id;
          });
        },

        /** Deselects list and disables the trashcan icon **/
        deselectAll = function() {
          documents.removeClass('selected');
          btnDeleteSelected.addClass('disabled');
        },

        /** User clicked the trashcan icon **/
        onClickDelete = function() {
          var title = '<span class="icon">&#xf071;</span> Delete Document',
              message = 'You cannot undo this operation. Are you sure you want to do this?',
              alert = new Alert(Alert.WARNING, title, message);

          alert.on('ok', deleteDocuments);
          return false;
        },

        storePageConfig = function() {
          var getQueryParam = function(param) {
                var q = window.location.search,
                    startIdx = q.indexOf(param + '='),
                    endIdx = (startIdx > -1) ?
                      Math.max(q.indexOf('&', startIdx), q.length) : -1;

                return (endIdx > -1) ? q.substring(startIdx + param.length + 1, endIdx) : undefined;
              },

              pageNumber = getQueryParam('p'),
              pageSize = getQueryParam('size'),

              storedLocationStr = localStorage.getItem('r2.my.location');
              storedLocation = (storedLocationStr) ? JSON.parse(storedLocationStr) : {};

          storedLocation.p = pageNumber;
          if (pageSize) storedLocation.size = pageSize;
          localStorage.setItem('r2.my.location', JSON.stringify(storedLocation));
        },

        /** Changing the sort order stores settings in localStorage and loads a new page **/
        onClickSort = function(e) {
          var el = jQuery(e.target).closest('td'),
              fieldName = el.data('field'),
              cssClass = el.attr('class'),
              sortOrder =
                (cssClass.indexOf('sorted') === -1) ? 'asc' : // Currently unsorted - use ASC
                (cssClass.indexOf('asc') > -1) ? 'desc' : 'asc', // If there's an order, toggle

              sorting = { sortby: fieldName, order: sortOrder },

              updateStoredLocation = function() {
                var storedAsString = localStorage.getItem('r2.my.location'),
                    stored = (storedAsString) ? JSON.parse(storedAsString) : {},
                    updated = jQuery.extend({}, stored, sorting);

                localStorage.setItem('r2.my.location', JSON.stringify(updated));
              };

          updateStoredLocation();
          URLUtils.setQueryParams(sorting);
        },

        /** Temporary: user clicked an icon representing an unimplemented feature **/
        onClickUnimplemented = function() {
          alert('This feature is not implemented yet (bear with us).');
          return false;
        },

        /**
         * Global click handler on the document, so we can
         * de-select if user clicks anywhere on the page
         */
        onClick = function(e) {
          var doc = getClickedDocument(e);
          if (doc) {
            if (!e.ctrlKey)
              deselectAll();

            btnDeleteSelected.removeClass('disabled');
            doc.addClass('selected');
          } else {
            // Click was outside the document list
            deselectAll();
          }
        },

        /** Deletes documents sequentially **/
        deleteDocuments = function() {
          var ids = getSelectedDocumentIDs(),
              head, tail;

          if (ids.length > 0) {
            head = ids[0];
            tail = ids.slice(1);

            jsRoutes.controllers.document.settings.SettingsController.deleteDocument(head).ajax()
              .fail(function(error) {
                console.log(error);
              })
              .done(function(result) {
                deleteDocuments(tail);
              });
          } else {
            window.location.reload(true);
          }
        },

        openDocument = function(e) {
          var id = getClickedDocument(e).data('id'),
              url = jsRoutes.controllers.document.annotation.AnnotationController
                      .showAnnotationView(id, 1).absoluteURL();

          window.location.href = url;
        };

    storePageConfig();
    formatTime();

    sortButtons.click(onClickSort);
    btnDeleteSelected.click(onClickDelete);

    // TODO temporary: register dummy handlers on icons for unimplemented features
    btnCreateFolder.click(onClickUnimplemented);
    btnGridView.click(onClickUnimplemented);
    searchbox.keyup(function(e) {
      if (e.which === 13)
        onClickUnimplemented();
    });

    // Register global click handler, so we can handle de-selects
    jQuery(document).click(onClick);

    // Double click on documents opens them
    jQuery('.document-panel').on('dblclick', '.document', openDocument);

    // If mobile, explicitely enable tap events alongside normal click events
    if (Config.IS_TOUCH) {
      Touch.enableTouchEvents();
      jQuery(document).on('tap', onClick);
      jQuery('.document-panel').on('doubletap', '.document', openDocument);
    }
  });

});
