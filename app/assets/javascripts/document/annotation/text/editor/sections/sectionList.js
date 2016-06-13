define([
  'document/annotation/text/editor/sections/comment/commentSection',
  'document/annotation/text/editor/sections/place/placeSection',
  'common/helpers/annotationUtils',
  'common/hasEvents'], function(CommentSection, PlaceSection, AnnotationUtils, HasEvents) {

  /** Represents a list of editor sections and provides utility methods **/
  var SectionList = function(containerEl) {

    var self = this,

        sections = [],

        queuedUpdates = [],

        currentAnnotation = false,

        /** Initializes the section list from the annotation **/
        setAnnotation = function(annotation) {
          clear();

          currentAnnotation = annotation;

          var placeBodies = AnnotationUtils.getBodiesOfType(annotation, 'PLACE'),
              commentBodies = AnnotationUtils.getBodiesOfType(annotation, 'COMMENT');

          jQuery.each(placeBodies, function(idx, placeBody) {
            initPlaceSection(placeBody);
          });

          jQuery.each(commentBodies, function(idx, commentBody) {
            initCommentSection(containerEl, commentBody);
          });
        },

        /** Common code for initializing a place section **/
        initPlaceSection = function(placeBody) {
          var quote = AnnotationUtils.getQuote(currentAnnotation),
              placeSection = new PlaceSection(containerEl, placeBody, quote);

          // Georesolution change needs to be handled by editor
          placeSection.on('change', function() {
            self.fireEvent('change', placeSection);
          });

          placeSection.on('delete', function() { removeSection(placeSection); });
          sections.push(placeSection);
        },

        /** Common code for initializing a comment section **/
        initCommentSection = function(containerEl, commentBody) {
          // TODO fix z-indexing
          var commentSection = new CommentSection(containerEl, commentBody);
          sections.push(commentSection);

          // Submit event needs to be handled by editor
          commentSection.on('submit', function() {
            self.fireEvent('submit');
          });

          commentSection.on('delete', function() { removeSection(commentSection); });
        },

        /** Creates a new section and queues creating in the annotation for later **/
        createNewSection = function(body) {
          if (body.type === 'PLACE') {
            initPlaceSection(body);
          }

          // TODO implement additional body types

          queuedUpdates.push(function() {
            currentAnnotation.bodies.push(body);
          });
        },

        /** Removes a section and queues removal of corresponding body for later **/
        removeSection = function(section) {
          // Destroy the section element, removing it from the DOM
          section.destroy();

          // Remove the section from the list
          var idx = sections.indexOf(section);
          if (idx > -1)
            sections.splice(idx, 1);

          // Queue the delete operation for later
          queuedUpdates.push(function() {
            AnnotationUtils.deleteBody(currentAnnotation, section.body);
          });
        },

        /** Returns the section for the specified body **/
        findForBody = function(body) {
          var matchingSections = jQuery.grep(sections, function(section) {
            return body === section.body;
          });

          if (matchingSections.length === 0)
            return false;
          else
            return matchingSections[0];
        },

        /**
         * Updates an existing section.
         *
         * Note: unlike for create/remove, we don't need to queue the change here.
         * Since this is just an update to an existing body, section.commit() will
         * handle the queued update.
         */
        updateSection = function(body, diff) {
          var sectionToUpdate = findForBody(body);
          if (sectionToUpdate)
            sectionToUpdate.update(diff);
        },

        /** Commits the changes that occured in the list **/
        commitChanges = function () {
          // Updates
          jQuery.each(sections, function(idx, section) {
            section.commit();
          });

          // Creates/deletes
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });
        },

        /** Destroys all sections **/
        clear = function() {
          currentAnnotation = false;

          jQuery.each(sections, function(idx, section) {
            section.destroy();
          });

          sections = [];
          queuedUpdates = [];
        };

    this.setAnnotation = setAnnotation;
    this.createNewSection = createNewSection;
    this.updateSection = updateSection;
    this.commitChanges = commitChanges;
    this.clear = clear;

    HasEvents.apply(this);
  };
  SectionList.prototype = Object.create(HasEvents.prototype);

  return SectionList;

});
