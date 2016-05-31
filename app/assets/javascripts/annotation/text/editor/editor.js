/**
 *
 * TODO there is now a hardcoded dependency between the editor and text selection functionality.
 * We'll need to break this dependency up in order to re-use the editor in image annotation mode.
 *
 */
define([
  'annotation/text/editor/replyField',
  'annotation/text/editor/georesolution/georesolutionPanel',
  'annotation/text/editor/sections/sectionList',
  'annotation/text/selection/highlighter',
  'annotation/text/selection/selectionHandler',
  'common/helpers/annotationUtils',
  'common/helpers/placeUtils',
  'common/autoPosition',
  'common/hasEvents'],

  function(ReplyField, GeoresolutionPanel, SectionList, Highlighter, SelectionHandler,
    AnnotationUtils, PlaceUtils, AutoPosition, HasEvents) {

  /** The main annotation editor popup **/
  var Editor = function(container) {

    var self = this,

        // Editor DOM element
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
                '<div class="sections"></div>' +
                '<div class="reply-container"></div>' +
                '<div class="footer">' +
                  '<button class="btn small outline cancel">Cancel</button>' +
                  '<button class="btn small outline ok-next">OK &amp Next</button>' +
                  '<button class="btn small ok">OK</button>' +
                '</div>' +
              '</div>' +
            '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        // DOM element shorthands
        sectionContainer = element.find('.sections'),
        replyContainer = element.find('.reply-container'),

        btnPlace = element.find('.category.place'),
        btnPerson = element.find('.category.person'),
        btnEvent = element.find('.category.event'),

        btnCancel = element.find('button.cancel'),
        btnOkAndNext = element.find('button.ok-next'),
        btnOk = element.find('button.ok'),

        // Renders highlights on the text
        highlighter = new Highlighter(container),

        // Monitors the page for selection events
        selectionHandler = new SelectionHandler(container, highlighter),

        // The dialog for changing place geo-resolution
        georesolutionPanel = new GeoresolutionPanel(),

        // The editor sections (to be initialized once the editor opens on a specific annotation)
        sectionList = new SectionList(sectionContainer),

        // The reply field
        replyField = new ReplyField(replyContainer),

        // The current annotation
        currentAnnotation = false,

        // The current annotation mode (NORMAL, QUICK or BULK)
        annotationMode = { mode: 'NORMAL' },

        setAnnotationMode = function(mode) {
          annotationMode = mode;
        },

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          clear();

          currentAnnotation = annotation;
          sectionList.setAnnotation(annotation);

          if (AnnotationUtils.countComments(annotation) > 0)
            replyField.setPlaceHolder('Write a reply...');

          element.show();
          AutoPosition.set(container, element, bounds);
        },

        /** Shorthand to check if the editor is currently open **/
        isOpen = function() {
          return element.is(':visible');
        },

        /** Clears all editor components **/
        clear = function() {
          sectionList.clear();
          replyField.clear();
          currentAnnotation = false;
        },

        /** Opens the editor on a newly created text selection **/
        editSelection = function(e) {
          var quote, record, body;

          if (annotationMode.mode === 'NORMAL') {
            open(e.annotation, e.bounds);

          } else if (annotationMode.mode === 'QUICK') {
            // Quick modes just add an annotation body and trigger instant OK
            currentAnnotation = e.annotation;

            if (annotationMode.type === 'PLACE') {
              PlaceUtils.createAnnotationBody(AnnotationUtils.getQuote(e.annotation))
                .done(function(body) {
                  e.annotation.bodies.push(body);
                  onOK();
                });

            } else if (annotationMode.type === 'PERSON') {
              e.annotation.bodies.push({ type: 'PERSON' });
              onOK();
            }
          } else if (annnotationMode.mode === 'BULK') {
            // TODO implement

          }

          return false;
        },

        /** Opens the editor on an existing annotation **/
        editAnnotation = function(eventOrElement) {
          var selection = selectionHandler.getSelection(),
              targetEl = (eventOrElement.target) ? eventOrElement.target: eventOrElement,
              allAnnotations;

          if (selection) {
            onSelectText(selection);
          } else {
            allAnnotations = highlighter.getAnnotationsAt(targetEl);
            open(allAnnotations[0], targetEl.getBoundingClientRect());
          }

          // To avoid repeated events from overlapping annotations below
          return false;
        },

        /** Moves the editor to the previous annotation **/
        toPreviousAnnotation = function() {
          var currentSpan = jQuery('*[data-id="' + currentAnnotation.annotation_id + '"]'),
              firstSpan = currentSpan[0],
              lastPrev = jQuery(firstSpan).prev('.annotation');

          if (lastPrev.length > 0)
            editAnnotation(lastPrev[0]);
        },

        /** Moves the editor to the next annotation **/
        toNextAnnotation = function() {
          var currentSpan = jQuery('*[data-id="' + currentAnnotation.annotation_id + '"]'),
              lastSpan = currentSpan[currentSpan.length - 1],
              firstNext = jQuery(lastSpan).next('.annotation');

          if (firstNext.length > 0)
            editAnnotation(firstNext[0]);
        },

        /** Click on 'Place' button adds a new PLACE body **/
        onAddPlace = function() {
          sectionList.createNewSection({ type: 'PLACE', status: { value: 'UNVERIFIED' } });
        },

        /** Click on 'Person' button adds a new PERSON body **/
        onAddPerson = function() {
          // TODO implemenet
        },

        /** Click on 'Event' button adds a new EVENT body **/
        onAddEvent = function() {
          // TODO implemenet
        },

        /** 'Cancel' clears the selection and closes the editor **/
        onCancel = function() {
          selectionHandler.clearSelection();
          sectionList.clear();
          element.hide();
        },

        /** 'OK' updates the annotation & highlight spans and closes the editor **/
        onOK = function() {
          var reply = replyField.getComment(), annotationSpans;

          sectionList.commitChanges();

          // Push the current reply body, if any
          if (reply)
            currentAnnotation.bodies.push(reply);

          // Determine which CRUD action to perform
          if (currentAnnotation.annotation_id) {
            // There's an ID - annotation already stored on the server
            if (AnnotationUtils.isEmpty(currentAnnotation)) {
              // Annotation empty - DELETE
              highlighter.removeAnnotation(currentAnnotation);
              self.fireEvent('deleteAnnotation', currentAnnotation);
            } else {
              // UPDATE
              annotationSpans = highlighter.refreshAnnotation(currentAnnotation);
              self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
            }
          } else {
            // No ID? New annotation from fresh selection - CREATE if not empty
            selectionHandler.clearSelection();
            if (!AnnotationUtils.isEmpty(currentAnnotation)) {
              annotationSpans = highlighter.renderAnnotation(currentAnnotation);
              self.fireEvent('updateAnnotation', { annotation: currentAnnotation, elements: annotationSpans });
            }
          }

          element.hide();
        },

        /** Shortcut: triggers OK and moves the editor to the next annotation **/
        onOKAndNext = function() {
          onOK();
          toPrevAnnotationAnnotation();
        },

        /** Handles keyboard shortcuts **/
        onKeyDown = function(e) {
          var key = e.which;

          if (key === 27)
            // ESC closes the editor
            onCancel();
          else if (key === 37 && isOpen())
            // Left arrow key
            toPreviousAnnotation();
          else if (key === 39 && isOpen())
            // Right arrow key
            toNextAnnotation();
        },

        /** User clicked 'change georesolution' - open the panel **/
        onChangeGeoresolution = function(section) {
          georesolutionPanel.open(AnnotationUtils.getQuote(currentAnnotation, section.body));
        },

        /** Georesolution was changed - forward changes to the section list **/
        onGeoresolutionChanged = function(placeBody, diff) {
          sectionList.updateSection(placeBody, diff);
        };

    // Monitor text selections through the selectionHandler
    selectionHandler.on('select', editSelection);

    // Monitor select of existing annotations via DOM
    jQuery(container).on('click', '.annotation', editAnnotation);

    // Keyboard shortcuts
    jQuery(document.body).keydown(onKeyDown);

    // Events from the section List
    sectionList.on('submit', onOK);
    sectionList.on('change', onChangeGeoresolution);

    // Georesolution change
    georesolutionPanel.on('change', onGeoresolutionChanged);

    // Ctrl+Enter on the reply field doubles as 'OK'
    replyField.on('submit', onOK);

    // Button events
    btnPlace.click(onAddPlace);
    btnPerson.click(onAddPerson);
    btnEvent.click(onAddEvent);

    btnCancel.click(onCancel);
    btnOk.click(onOK);
    btnOkAndNext.click(onOKAndNext);

    this.setAnnotationMode = setAnnotationMode;

    HasEvents.apply(this);
  };
  Editor.prototype = Object.create(HasEvents.prototype);

  return Editor;

});
