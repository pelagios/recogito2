require([], function() {

  jQuery(document).ready(function() {
    var documentPane = jQuery('.document-pane'),

        documents = jQuery('.document'),

        getDocumentEl = function(e) {
          var docEl = jQuery(e.target).closest('.document');
          if (docEl.length > 0)
            return docEl;
          else
            return false;
        },

        deselectAll = function() {
          documents.removeClass('selected');
        },

        onClick = function(e) {
          var doc = getDocumentEl(e);

          if (doc) {
            if (!e.ctrlKey)
              deselectAll();

            doc.addClass('selected');
          } else {
            // Click was outside the document list
            deselectAll();
          }
        },

        openDocument = function(e) {
          window.location.href = getDocumentEl(e).data('href');
        },

        deleteDocument = function(id) {
          jsRoutes.controllers.document.DocumentController.deleteDocument(id).ajax()
                  .done(function(result) {
                    window.location.reload(true);
                  })
                  .fail(function(error) {
                    console.log(error);
                  });
        };

    jQuery(document).click(onClick);
    documentPane.on('dblclick', '.document', openDocument);
  });

});
