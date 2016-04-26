define(['../../../common/annotationUtils',
        '../../../common/hasEvents',
        'highlighter',
        'editor/selectionHandler',
        'editor/components/placeSection',
        'editor/components/commentSection'], function(Utils, HasEvents, Highlighter, SelectionHandler, PlaceSection, CommentSection) {

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

        commentSection = new CommentSection(bodyContainer),

        btnPlace = element.find('.category.place'),
        btnPerson = element.find('.category.person'),
        btnEvent = element.find('.category.event'),

        btnCancel = element.find('button.cancel'),
        btnOk = element.find('button.ok'),

        currentAnnotation = false,

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          currentAnnotation = annotation;

          // TODO populate the template

          element.css({ top: bounds.bottom, left: bounds.left });
          element.show();
        },

        /** Closes the editor, cleaning up all components **/
        close = function() {
          element.hide();
          selectionHandler.clearSelection();
        },

        clear = function() {
          currentAnnotation = false;

          // Destroy body sections
          jQuery.each(bodySections, function(idx, section) {
            section.destroy();
          });

          // Clear comment field & text selection, if any
          commentSection.clear();
        },

        /** Selecting text (i.e. creating a new annotation) opens the editor **/
        onSelectText = function(e) {
          open(e.annotation, e.bounds);
        },

        onSelectAnnotation = function(e) {
          var annotation = e.target.annotation,
              quote = Utils.getQuote(annotation);

          console.log(quote);

          // To avoid repeated events from overlapping annotations below
          return false;
        },

        onAddPlace = function() {
          // Add a place body stub to the annotation
          var placeBodyStub = { type: 'PLACE' },
              placeSection = new PlaceSection(bodyContainer, placeBodyStub);

          bodySections.push(placeSection);
          placeSection.automatch(Utils.getQuote(annotationStub));
          currentAnnotation.bodies.push(placeBodyStub);
        },

        onAddPerson = function() {
          // TODO implement
        },

        onAddEvent = function() {
          // TODO implement
        },

        onOk = function() {
          var comment = commentSection.getComment();
          if (comment)
            currentAnnotation.bodies.push(comment);

          highlighter.renderAnnotation(currentAnnotation);

          close();

          self.fireEvent('updateAnnotation', currentAnnotation);
        },

        onCancel = function() {
          close();
        };

    // Monitor text selections through the selectionHandler
    selectionHandler.on('select', onSelectText);

    // Monitor select of existing annotations via DOM
    jQuery(parentNode).on('click', '.annotation', onSelectAnnotation);

    // Ctrl+Enter on comment section doubles as OK
    commentSection.on('submit', onOk);

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
