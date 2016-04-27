define(['../../../common/annotationUtils',
        '../../../common/hasEvents',
        'highlighter',
        'editor/selectionHandler',
        'editor/components/commentSection',
        'editor/components/placeSection',
        'editor/components/replyField'], function(Utils, HasEvents, Highlighter, SelectionHandler, CommentSection, PlaceSection, ReplyField) {

  /** The main annotation editor popup **/
  var Editor = function(parentNode) {

    var self = this,

        highlighter = new Highlighter(parentNode),

        selectionHandler = new SelectionHandler(parentNode, highlighter),

        element = (function() {
          var el = jQuery(
            '<div class="text-annotation-editor">' +
              '<div class="arrow"></div>' +
              '<div class="text-annotation-editor-inner">' +
                '<div class="category-buttons">' +
                  '<div class="category-container">' +
                    '<div class="category place">' +
                      '<span class="icon">&#xf041;</span> Place' +
                    '</div>' +
                  '</div>' +
                  '<div class="category-container">' +
                    '<div class="category person">' +
                      '<span class="icon">&#xf007;</span> Person' +
                    '</div>' +
                  '</div>' +
                  '<div class="category-container">' +
                    '<div class="category event">' +
                      '<span class="icon">&#xf005;</span> Event' +
                    '</div>' +
                  '</div>' +
                '</div>' +
                '<div class="bodies">' +
                  '<div class="bodies-place"></div>' +
                  '<div class="bodies-comment"></div>' +
                '</div>' +
                '<div class="fields"></div>' +
                '<div class="footer">' +
                  '<button class="btn small outline cancel">Cancel</button>' +
                  '<button class="btn small ok">OK</button>' +
                '</div>' +
              '</div>' +
            '</div>');

          jQuery(parentNode).append(el);
          el.hide();
          return el;
        })(),

        bodyContainer = element.find('.bodies'),
        placeBodyContainer = bodyContainer.find('.bodies-place'),
        commentBodyContainer = bodyContainer.find('.bodies-comment'),

        bodySections = [],

        fieldContainer = element.find('.fields'),
        replyField = new ReplyField(fieldContainer),

        btnPlace = element.find('.category.place'),
        btnPerson = element.find('.category.person'),
        btnEvent = element.find('.category.event'),

        btnCancel = element.find('button.cancel'),
        btnOk = element.find('button.ok'),

        currentAnnotation = false,

        /** Removes a body + corresponding section **/
        removeBody = function(section, body) {
          // Remove the body from the annotation
          Utils.deleteBody(currentAnnotation, body);

          // Destroy the section element, removing it from the DOM
          section.destroy();

          // Remove the section from the list
          var idx = bodySections.indexOf(section);
          if (idx > -1)
            bodySections.splice(idx, 1);
        },

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          var scrollTop = jQuery(document).scrollTop(),
              quote = Utils.getQuote(annotation);

          clear();
          currentAnnotation = annotation;

          // Add place body sections
          var places = Utils.getBodiesOfType(annotation, 'PLACE');
          jQuery.each(places, function(idx, placeBody) {
            bodySections.push(new PlaceSection(placeBodyContainer, placeBody, quote));
          });

          // Add comment body sections
          var comments = Utils.getBodiesOfType(annotation, 'COMMENT');
          jQuery.each(comments, function(idx, commentBody) {
            var zIndex = comments.length - idx,
                commentSection = new CommentSection(commentBodyContainer, commentBody, zIndex);

            bodySections.push(commentSection);
            commentSection.on('delete', function() { removeBody(commentSection, commentBody); });
          });

          if (comments.length > 0)
            replyField.setPlaceHolder('Write a reply...');

          element.css({ top: bounds.bottom + scrollTop, left: bounds.left });
          element.show();
        },

        /** Closes the editor, clearing all editor components **/
        close = function() {
          element.hide();
          clear();
        },

        /** Clears all editor components **/
        clear = function() {
          // Destroy body sections
          jQuery.each(bodySections, function(idx, section) {
            section.destroy();
          });

          // Clear reply field & text selection, if any
          replyField.clear();
          currentAnnotation = false;
        },

        /** Opens the editor on a newly created text selection **/
        onSelectText = function(e) {
          open(e.annotation, e.bounds);
        },

        /** Opens the editor on an existing annotation **/
        onSelectAnnotation = function(e) {
          var annotation = e.target.annotation,
              bounds = e.target.getBoundingClientRect();

          open(annotation, bounds);

          // TODO perhaps we need to check for the 'topmost' annotation?

          // To avoid repeated events from overlapping annotations below
          return false;
        },

        /** Click on 'Place' button adds a place body **/
        onAddPlace = function() {
          var placeBody = { type: 'PLACE', status: { value: 'UNVERIFIED' } },
              quote = Utils.getQuote(currentAnnotation),
              placeSection = new PlaceSection(placeBodyContainer, placeBody, quote);

          currentAnnotation.bodies.push(placeBody);
          bodySections.push(placeSection);
        },

        /** TODO implement **/
        onAddPerson = function() {

        },

        /** TODO implement **/
        onAddEvent = function() {

        },

        /** Stores the annotation (converting selection to annotation highlight, if any) **/
        storeAnnotation = function() {
          var annotationSpans;

          if (currentAnnotation.annotation_id) {
            // Annotation is already stored at the server - update
            annotationSpans = jQuery('[data-id=' + currentAnnotation.annotation_id + ']');
          } else {
            // New annotation, created from a fresh selection
            selectionHandler.clearSelection();
            annotationSpans = highlighter.renderAnnotation(currentAnnotation);
          }

          self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
        },

        /**
         * 'OK' adds the content of the reply field as comment, stores the
         * annotation (which will clear the selection) and closes the editor.
         */
        onOk = function() {
          var reply = replyField.getComment();
          if (reply)
            currentAnnotation.bodies.push(reply);

          storeAnnotation();
          close();
        },

        /** 'Cancel' clears the selection and closes the editor **/
        onCancel = function() {
          selectionHandler.clearSelection();
          close();
        };

    // Monitor text selections through the selectionHandler
    selectionHandler.on('select', onSelectText);

    // Monitor select of existing annotations via DOM
    jQuery(parentNode).on('click', '.annotation', onSelectAnnotation);

    // Ctrl+Enter on reply field doubles as 'OK'
    replyField.on('submit', onOk);

    // Wire button events
    btnPlace.click(onAddPlace);
    btnPerson.click(onAddPerson);
    btnEvent.click(onAddEvent);
    btnCancel.click(onCancel);
    btnOk.click(onOk);

    HasEvents.apply(this);
  };
  Editor.prototype = Object.create(HasEvents.prototype);

  return Editor;

});
