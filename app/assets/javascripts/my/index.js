require([], function() {

  jQuery(document).ready(function() {
        /** Trashcan icon **/
    var trashcan = jQuery('button.delete'),

        /** Document elements **/
        documents = jQuery('.document'),

        /** Resolves the click target to the parent document element **/
        getClickedDocument = function(e) {
          var docEl = jQuery(e.target).closest('.document');
          if (docEl.length > 0)
            return docEl;
          else
            return false;
        },

        deselectAll = function() {
          documents.removeClass('selected');
          trashcan.addClass('disabled');
        },

        getSelectedDocumentIDs = function() {
          var selected = jQuery.grep(documents, function(docEl) {
            return jQuery(docEl).hasClass('selected');
          });

          return jQuery.map(selected, function(el) {
            return el.dataset.id;
          });
        },

        /**
         * Global click handler on the document, so we can
         * de-select if user clicks anywhere on the page
         */
        onClick = function(e) {
          var trashcanClicked = e.target === trashcan[0],
              doc = getClickedDocument(e);

          if (trashcanClicked) {
            deleteDocuments(getSelectedDocumentIDs());
          } else {
            if (doc) {
              if (!e.ctrlKey)
                deselectAll();

              trashcan.removeClass('disabled');
              doc.addClass('selected');
            } else {
              // Click was outside the document list
              deselectAll();
            }
          }
        },

        /** Deletes documents sequentially **/
        deleteDocuments = function(ids) {
          var head, tail;

          if (ids.length > 0) {
            head = ids[0];
            tail = ids.slice(1);

            jsRoutes.controllers.document.DocumentController.deleteDocument(head).ajax()
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
                      .showAnnotationViewForDocPart(id, 1).absoluteURL();

          window.location.href = url;
        };

    jQuery(document).click(onClick);
    jQuery('.document-panel').on('dblclick', '.document', openDocument);
  });

});
