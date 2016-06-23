/**
 *
 * TODO there is now a hardcoded dependency between the editor and text selection functionality.
 * We'll need to break this dependency up in order to re-use the editor in image annotation mode.
 *
 */
define([
  'document/annotation/text/editor/autoPosition',
  'document/annotation/text/editor/sections/sectionList',
  'document/annotation/text/selection/highlighter',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'common/config',
  'common/hasEvents'],

  function(AutoPosition, SectionList, Highlighter,
    AnnotationUtils, PlaceUtils, API, Config, HasEvents) {

  /** The main annotation editor popup **/
  var Editor = function(container) {

    var self = this,

        // Editor DOM element
        element = (function() {
          var el = jQuery(
                '<div class="text-annotation-editor">' +
                  '<div class="arrow"></div>' +
                  '<div class="text-annotation-editor-inner">' +
                    '<div class="sections"></div>' +
                    '<div class="footer">' +
                      '<button class="btn small close">Close</button>' +
                    '</div>' +
                  '</div>' +
                '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        // DOM element shorthands
        sectionContainer = element.find('.sections'),

        btnClose = element.find('button.close'),

        // Renders highlights on the text
        highlighter = new Highlighter(container),

        // The editor sections (to be initialized once the editor opens on a specific annotation)
        sectionList = new SectionList(sectionContainer),

        // The current annotation
        currentAnnotation = false,

        /** Opens the editor with an annotation, at the specified bounds **/
        open = function(annotation, bounds) {
          clear();

          currentAnnotation = annotation;
          sectionList.setAnnotation(annotation);

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
          currentAnnotation = false;
        },

        /** Opens the editor on an existing annotation **/
        editAnnotation = function(eventOrElement) {
          var element = (eventOrElement.target) ? eventOrElement.target: eventOrElement,
              allAnnotations = highlighter.getAnnotationsAt(element);

          open(allAnnotations[0], element.getBoundingClientRect());

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

        /** 'OK' updates the annotation & highlight spans and closes the editor **/
        onClose = function() {
          element.hide();
        },

        /** Handles keyboard shortcuts **/
        onKeyDown = function(e) {
          var key = e.which;

          if (key === 27)
            // ESC closes the editor
            if (Config.writeAccess)
              onCancel();
            else
              onOK();
          else if (key === 37 && isOpen())
            // Left arrow key
            toPreviousAnnotation();
          else if (key === 39 && isOpen())
            // Right arrow key
            toNextAnnotation();
        };

    // Monitor select of existing annotations via DOM
    jQuery(container).on('click', '.annotation', editAnnotation);

    // Keyboard shortcuts
    jQuery(document.body).keydown(onKeyDown);

    btnClose.click(onClose);

    HasEvents.apply(this);
  };
  Editor.prototype = Object.create(HasEvents.prototype);

  return Editor;

});
