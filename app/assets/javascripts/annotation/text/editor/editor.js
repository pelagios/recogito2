define(['../../../common/annotationUtils',
        '../../../common/placeUtils',
        '../../../common/hasEvents',
        'highlighter',
        'editor/selectionHandler',
        'editor/components/commentSection',
        'editor/components/placeSection',
        'editor/components/replyField'], function(AnnotationUtils,
                                                  PlaceUtils,
                                                  HasEvents,
                                                  Highlighter,
                                                  SelectionHandler,
                                                  CommentSection,
                                                  PlaceSection,
                                                  ReplyField) {

  /** The main annotation editor popup **/
  var Editor = function(parentNode) {

    var self = this,

        annotationMode = { mode: 'NORMAL' },

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

        /** Currently visible body sections **/
        bodySections = [],

        /** Changes to be applied to the annotation when the user clicks OK **/
        queuedUpdates = [],

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
          // Destroy the section element, removing it from the DOM
          section.destroy();

          // Remove the section from the list
          var idx = bodySections.indexOf(section);
          if (idx > -1)
            bodySections.splice(idx, 1);

          // Queue the delete operation for later, when user click 'OK'
          queuedUpdates.push(function() { AnnotationUtils.deleteBody(currentAnnotation, body); });
        },

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          var scrollTop = jQuery(document).scrollTop(),
              quote = AnnotationUtils.getQuote(annotation);

          clear();
          currentAnnotation = annotation;

          // Add place body sections
          var places = AnnotationUtils.getBodiesOfType(annotation, 'PLACE');
          jQuery.each(places, function(idx, placeBody) {
            bodySections.push(new PlaceSection(placeBodyContainer, placeBody, quote));
          });

          // Add comment body sections
          var comments = AnnotationUtils.getBodiesOfType(annotation, 'COMMENT');
          jQuery.each(comments, function(idx, commentBody) {
            var zIndex = comments.length - idx,
                commentSection = new CommentSection(commentBodyContainer, commentBody, zIndex);

            bodySections.push(commentSection);
            commentSection.on('delete', function() { removeBody(commentSection, commentBody); });
            commentSection.on('submit', onOk);
          });

          if (comments.length > 0)
            replyField.setPlaceHolder('Write a reply...');

          element.css({ top: bounds.bottom + scrollTop, left: bounds.left });
          element.show();
        },

        setMode = function(mode) {
          annotationMode = mode;
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

          bodySections = [];

          // Clear reply field & text selection, if any
          replyField.clear();
          currentAnnotation = false;
        },

        /** Opens the editor on a newly created text selection **/
        onSelectText = function(e) {
          var quote, record, body;

          if (annotationMode.mode === 'NORMAL') {
            open(e.annotation, e.bounds);

          } else if (annotationMode.mode === 'QUICK' && annotationMode.type === 'PLACE') {
            quote = AnnotationUtils.getQuote(e.annotation);
            currentAnnotation = e.annotation;
            body = { type: 'PLACE', status: { value: 'UNVERIFIED' } };

            PlaceUtils.findMatches(quote).done(function(response) {
              if (response.total > 0) {
                record = PlaceUtils.getBestMatchingRecord(response.items[0], quote);
                body.uri = record.uri;
              }

              currentAnnotation.bodies.push(body);
              onOk();
            });

          } else if (annotationMode.mode === 'QUICK' && annotationMode.type === 'PERSON') {
            currentAnnotation = e.annotation;
            currentAnnotation.bodies.push({ type: 'PERSON' });
            onOk();
          }

          // TODO what happens on mode == bulk?

          return false;
        },

        /** Opens the editor on an existing annotation **/
        onSelectAnnotation = function(e) {
          var selection = selectionHandler.getSelection(),
              allAnnotations, topAnnotation, bounds,

              /** This helper gets all annotations in case of multipe nested annotation spans **/
              getAnnotationsRecursive = function(element, a) {
                var annotations = (a) ? a : [ ],
                    parent = element.parentNode;

                annotations.push(element.annotation);

                if (jQuery(parent).hasClass('annotation'))
                  return getAnnotationsRecursive(parent, annotations);
                else
                  return annotations;
              },

              /** Helper that sorts annotations by quote length, so we can pick the shortest **/
              sortByQuoteLength = function(annotations) {
                return annotations.sort(function(a, b) {
                  return AnnotationUtils.getQuote(a).length - AnnotationUtils.getQuote(b).length;
                });
              };

          if (selection) {
            onSelectText(selection);
          } else {
            allAnnotations = sortByQuoteLength(getAnnotationsRecursive(e.target));
            topAnnotation = allAnnotations[0];
            bounds = e.target.getBoundingClientRect();
            open(topAnnotation, bounds);
          }

          // To avoid repeated events from overlapping annotations below
          return false;
        },

        /** Click on 'Place' button adds a place body **/
        onAddPlace = function() {
          var placeBody = { type: 'PLACE', status: { value: 'UNVERIFIED' } },
              quote = AnnotationUtils.getQuote(currentAnnotation),
              placeSection = new PlaceSection(placeBodyContainer, placeBody, quote);

          bodySections.push(placeSection);

          queuedUpdates.push(function() {
            currentAnnotation.bodies.push(placeBody);
          });
        },

        /** TODO implement **/
        onAddPerson = function() {

        },

        /** TODO implement **/
        onAddEvent = function() {

        },

        /**
         * 'OK' adds the content of the reply field as comment, stores the annotation, updates
         * selection/annotation highlight if necessary and closes the editor.
         */
        onOk = function() {
          var reply = replyField.getComment(),
              bodies = currentAnnotation.bodies,
              annotationSpans;

          // Commit all changes that may have happend inside sections
          jQuery.each(bodySections, function(idx, section) {
            section.commit();
          });

          // Push the current reply body, if any
          if (reply)
            currentAnnotation.bodies.push(reply);

          // Apply queued changes
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });

          // Determine which CRUD action to perform
          if (currentAnnotation.annotation_id) {
            // There's an ID - annotation already stored on the server
            if (bodies.length === 1 && bodies[0].type === 'QUOTE') {
              // Annotation has no bodies left except the QUOTE - DELETE
              highlighter.removeAnnotation(currentAnnotation);
              self.fireEvent('deleteAnnotation', currentAnnotation);
            } else {
              // UPDATE
              annotationSpans = jQuery('[data-id=' + currentAnnotation.annotation_id + ']');
              self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
            }
          } else {
            // No ID? New annotation from fresh selection - CREATE
            selectionHandler.clearSelection();
            annotationSpans = highlighter.renderAnnotation(currentAnnotation);
            self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
          }

          close();
        },

        /** 'Cancel' clears the selection and closes the editor **/
        onCancel = function() {
          selectionHandler.clearSelection();

          // Discard changes
          queuedUpdates = [];

          close();
        };

    // Monitor text selections through the selectionHandler
    selectionHandler.on('select', onSelectText);

    // Monitor select of existing annotations via DOM
    jQuery(parentNode).on('click', '.annotation', onSelectAnnotation);

    // Ctrl+Enter on reply field doubles as 'OK'
    replyField.on('submit', onOk);

    // ESC closes the editor
    jQuery(document).keyup(function(e) {
      if (e.which === 27)
        onCancel();
    });

    // Wire button events
    btnPlace.click(onAddPlace);
    btnPerson.click(onAddPerson);
    btnEvent.click(onAddEvent);
    btnCancel.click(onCancel);
    btnOk.click(onOk);

    this.setMode = setMode;

    HasEvents.apply(this);
  };
  Editor.prototype = Object.create(HasEvents.prototype);

  return Editor;

});
