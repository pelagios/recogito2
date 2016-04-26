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
                '<div class="bodies"></div>' +
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
        bodySections = [],

        fieldContainer = element.find('.fields'),
        replyField = new ReplyField(fieldContainer),

        btnPlace = element.find('.category.place'),
        btnPerson = element.find('.category.person'),
        btnEvent = element.find('.category.event'),

        btnCancel = element.find('button.cancel'),
        btnOk = element.find('button.ok'),

        currentAnnotation = false,

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          var scrollTop = jQuery(document).scrollTop();

          clear();
          currentAnnotation = annotation;

          // Add place body sections

          // Add comment body sections
          var comments = Utils.getBodiesOfType(annotation, 'COMMENT');
          jQuery.each(comments, function(idx, commentBody) {
            bodySections.push(new CommentSection(bodyContainer, commentBody));
          });
          if (comments.length > 0)
            replyField.setPlaceHolder('Write a reply...');

          element.css({ top: bounds.bottom + scrollTop, left: bounds.left });
          element.show();
        },

        /** Closes the editor, cleaning up all components **/
        close = function() {
          element.hide();
          clear();
        },

        clear = function() {
          // Destroy body sections
          jQuery.each(bodySections, function(idx, section) {
            section.destroy();
          });

          // Clear reply field & text selection, if any
          replyField.clear();

          currentAnnotation = false;
        },

        /** Selecting text (i.e. creating a new annotation) opens the editor **/
        onSelectText = function(e) {
          open(e.annotation, e.bounds);
        },

        onSelectAnnotation = function(e) {
          var annotation = e.target.annotation,
              bounds = e.target.getBoundingClientRect();

          open(annotation, bounds);

          // To avoid repeated events from overlapping annotations below
          return false;
        },

        onAddPlace = function() {
          // Add a place body stub to the annotation
          var placeBodyStub = { type: 'PLACE', status: { value: 'UNVERIFIED' } },
              placeSection = new PlaceSection(bodyContainer, placeBodyStub);

          bodySections.push(placeSection);
          placeSection.automatch(Utils.getQuote(currentAnnotation));
          currentAnnotation.bodies.push(placeBodyStub);
        },

        onAddPerson = function() {
          // TODO implement
        },

        onAddEvent = function() {
          // TODO implement
        },

        onOk = function() {
          var reply = replyField.getComment(),
              annotationSpans;

          if (reply)
            currentAnnotation.bodies.push(reply);

          if (currentAnnotation.annotation_id) {
            // Annotation is already stored at the server - update
            annotationSpans = jQuery('[data-id=' + currentAnnotation.annotation_id + ']');
          } else {
            // New annotation, created from a fresh selection
            selectionHandler.clearSelection();
            annotationSpans = highlighter.renderAnnotation(currentAnnotation);
          }

          self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
          close();
        },

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
