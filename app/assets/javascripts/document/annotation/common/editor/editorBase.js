/**
 *
 * TODO there is now a hardcoded dependency between the editor and text highlight functionality.
 * We'll need to break this dependency up in order to re-use the editor in image annotation mode.
 *
 */
define([
  'common/utils/annotationUtils',
  'common/hasEvents',
  'document/annotation/common/editor/sections/sectionList'
], function(AnnotationUtils, HasEvents, SectionList) {

  var EditorBase = function(container, element, opts) {
    var self = this,

        /** Handles common key events **/
        onKeyDown = function(e) {
          var key = e.which;

          if (key === 27)
            // ESC
            self.fireEvent('escape');
        };

    if (!this.openSelection)
      throw 'Editor needs to implement .openSelection() method';

    // Fields accessible to prototype methods
    this.container = container;
    this.element = element;

    this.autoscroll = (opts && opts.autoscroll) ? opts.autoscroll : true;

    this.sectionList = new SectionList(element);
    this.currentSelection = false;

    // Monitor key events
    jQuery(document.body).keydown(onKeyDown);

    HasEvents.apply(this);
  };
  EditorBase.prototype = Object.create(HasEvents.prototype);

  EditorBase.prototype.setPosition = function(bounds) {
    var self = this,
        scrollTop = jQuery(document).scrollTop(),
        offset = jQuery(this.container).offset(),
        windowHeight = jQuery(window).height(),

        // Fixes bounds to take into account text container offset and scroll
        translatedBounds = {
          bottom: bounds.bottom - offset.top + scrollTop,
          height: bounds.height,
          left: bounds.left - offset.left,
          right: bounds.right - offset.left,
          top: bounds.top - offset.top + scrollTop,
          width: bounds.width
        },

        rectBefore, rectAfter;

    // Default orientation
    this.element.css({ top: translatedBounds.bottom, left: translatedBounds.left, bottom: 'auto' });
    rectBefore = this.element[0].getBoundingClientRect();

    // Flip horizontally, if popup exceeds screen width
    if (rectBefore.right > jQuery(window).width()) {
      this.element.addClass('align-right');
      this.element.css('left', translatedBounds.right - self.element.width());
    } else {
      this.element.removeClass('align-right');
    }

    // Flip vertically if popup exceeds screen height
    if (rectBefore.bottom > windowHeight) {
      this.element.addClass('align-bottom');
      this.element.css({ top: 'auto', bottom: self.container.clientHeight - translatedBounds.top });
    } else {
      this.element.removeClass('align-bottom');
    }

    // Still not visible? Autoscroll (if enabled)
    if (this.autoscroll) {
      rectAfter = this.element[0].getBoundingClientRect();
      if (rectAfter.bottom > windowHeight || rectAfter.top < 100)
        jQuery(document.body).scrollTop(50 + scrollTop + rectAfter.bottom - windowHeight);
    }
  };

  EditorBase.prototype.open = function(selection) {
    if (selection) {
      this.clear();
      this.currentSelection = selection;
      this.sectionList.setAnnotation(selection.annotation);
      this.element.show();
      this.setPosition(selection.bounds);
      if (selection.annotation.annotation_id)
        history.pushState(null, null, '#' + selection.annotation.annotation_id);
        // window.location.hash = '#' + selection.annotation.annotation_id;
      else
        history.pushState(null, null, window.location.pathname);
        // window.location.hash = '';
    } else {
      // We allow this method to be called with no arg - in this case, close the editor
      this.close();
    }
  };

  /** Shorthand to check if the editor is currently open **/
  EditorBase.prototype.isOpen = function() {
    return this.element.is(':visible');
  };

  /**
   * Helper to retrieve the most recent context (quote or transcription). Since the editor
   * fields may contain info that is not yet stored in the annotation bodies, we need to
   * obtain this from the editor directly.
   *
   * It is (sensibly) assumed that annotations will either have one QUOTE (text) or
   * multiple TRANSCRIPTION (image) bodies, never both.
   */
  EditorBase.prototype.getMostRecentContext = function() {
    var quote = AnnotationUtils.getQuote(this.currentSelection.annotation);
    if (quote)
      return quote;
    else
      return this.sectionList.getMostRecentTranscription();
  };

  EditorBase.prototype.close = function() {
    if (this.isOpen()) {
      history.pushState(null, null, window.location.pathname);
      // window.location.hash = '';
      this.clear();
      this.element.hide();
    }
  };

  EditorBase.prototype.clear = function() {
    this.sectionList.clear();
    this.currentSelection = false;
  };

  return EditorBase;

});
