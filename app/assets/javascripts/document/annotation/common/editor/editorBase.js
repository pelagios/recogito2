/**
 *
 * TODO there is now a hardcoded dependency between the editor and text highlight functionality.
 * We'll need to break this dependency up in order to re-use the editor in image annotation mode.
 *
 */
define([
  'document/annotation/common/editor/sections/sectionList',
  'common/hasEvents'], function(SectionList, HasEvents) {

  var EditorBase = function(container, element) {
    var self = this,

        /** Handles common key events **/
        onKeyDown = function(e) {
          var key = e.which;

          if (key === 27)
            // ESC
            self.fireEvent('escape');
          else if (key === 37 && self.isOpen())
            // Left arrow key
            self.toPreviousAnnotation();
          else if (key === 39 && self.isOpen())
            // Right arrow key
            self.toNextAnnotation();
        };

    if (!this.openSelection)
      throw 'Editor needs to implement .openSelection() method';

    // Fields accessible to prototype methods
    this.container = container;
    this.element = element;
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

    // Still not visible? Scroll down
    rectAfter = this.element[0].getBoundingClientRect();
    if (rectAfter.bottom > windowHeight || rectAfter.top < 100) {
      jQuery(document.body).scrollTop(50 + scrollTop + rectAfter.bottom - windowHeight);
    }
  };

  EditorBase.prototype.open = function(selection) {
    this.clear();
    this.currentSelection = selection;
    this.sectionList.setAnnotation(selection.annotation);
    this.element.show();
    this.setPosition(selection.bounds);
  };

  /** Shorthand to check if the editor is currently open **/
  EditorBase.prototype.isOpen = function() {
    return this.element.is(':visible');
  };

  EditorBase.prototype.clear = function() {
    this.sectionList.clear();
    this.currentSelection = false;
  };

  /**
   * Moves the editor to the previous annotation
   *
   * TODO this is a hard-wired dependency to the text annotation UI - REFACTOR!
   *
  EditorBase.prototype.toPreviousAnnotation = function() {
    var currentSpan = jQuery('*[data-id="' + this.currentAnnotation.annotation_id + '"]'),
        firstSpan = currentSpan[0],
        lastPrev = jQuery(firstSpan).prev('.annotation'),
        previousAnnotations;

    if (lastPrev.length > 0) {
      previousAnnotations = this.highlighter.getAnnotationsAt(lastPrev[0]);
      this.open(previousAnnotations[0], lastPrev[0].getBoundingClientRect());
    }
  };

  /**
   * Moves the editor to the next annotation
   *
   * TODO this is a hard-wired dependency to the text annotation UI - REFACTOR!
   *
  EditorBase.prototype.toNextAnnotation = function() {
    var currentSpan = jQuery('*[data-id="' + this.currentAnnotation.annotation_id + '"]'),
        lastSpan = currentSpan[currentSpan.length - 1],
        firstNext = jQuery(lastSpan).next('.annotation'),
        nextAnnotations;

    if (firstNext.length > 0) {
      nextAnnotations = this.highlighter.getAnnotationsAt(firstNext[0]);
      this.open(nextAnnotations[0], firstNext[0].getBoundingClientRect());
    }
  };
  */

  return EditorBase;

});
