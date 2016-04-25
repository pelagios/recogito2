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

        annotationStub = false,

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

        onSelect = function(e) {
          annotationStub = e.annotation;
          element.css({ top: e.bounds.bottom, left: e.bounds.left });
          element.show();
        },

        close = function() {
          jQuery.each(bodySections, function(idx, section) {
            section.destroy();
          });

          commentSection.clear();
          selectionHandler.clearSelection();
          element.hide();
        },

        onAddPlace = function() {
          // Add a place body stub to the annotation
          var placeBodyStub = { type: 'PLACE' },
              placeSection = new PlaceSection(bodyContainer, placeBodyStub);

          annotationStub.bodies.push(placeBodyStub);
          bodySections.push(placeSection);
          placeSection.automatch(Utils.getQuote(annotationStub));
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
            annotationStub.bodies.push(comment);

          close();

          highlighter.renderAnnotation(annotationStub);
          self.fireEvent('updateAnnotation', annotationStub);
        },

        onCancel = function() {
          close();
        };

    selectionHandler.on('select', onSelect);

    // Ctrl+Enter on comment section doubles as OK
    commentSection.on('submit', onOk);

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
