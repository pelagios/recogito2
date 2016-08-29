define([
  'document/annotation/common/editor/sections/comment/commentSection',
  'document/annotation/common/editor/sections/place/placeSection',
  'document/annotation/common/editor/sections/transcription/transcriptionSection',
  'document/annotation/common/editor/textEntryField',
  'common/utils/annotationUtils',
  'common/config',
  'common/hasEvents'],

function(
  CommentSection,
  PlaceSection,
  TranscriptionSection,
  TextEntryField,
  AnnotationUtils,
  Config,
  HasEvents) {

  /** Represents a list of editor sections and provides utility methods **/
  var SectionList = function(editorEl) {
    var self = this,

        transcriptionSectionEl = editorEl.find('.transcription-sections'),

        centerSectionEl = editorEl.find('.center-sections'),

        sections = [],

        queuedUpdates = [],

        currentAnnotation = false,

        /** Initializes the section list from the annotation **/
        setAnnotation = function(annotation) {
          var transcriptionBodies = AnnotationUtils.getBodiesOfType(annotation, 'TRANSCRIPTION'),
              placeBodies = AnnotationUtils.getBodiesOfType(annotation, 'PLACE'),
              commentBodies = AnnotationUtils.getBodiesOfType(annotation, 'COMMENT');

          clear();

          currentAnnotation = annotation;

          // Transcriptions exist only on image content
          if (Config.contentType.indexOf('IMAGE') === 0)
            initTranscriptionSections(transcriptionBodies);

          jQuery.each(placeBodies, function(idx, placeBody) {
            // TODO what about toponym arg?
            initPlaceSection(placeBody);
          });

          jQuery.each(commentBodies, function(idx, commentBody) {
            initCommentSection(commentBody);
          });
        },

        /** Shorthand for attaching the standard section delete handler **/
        handleDelete = function(section) {
          section.on('delete', function() { removeSection(section); });
        },

        /** Shorthand for forwarding an event to the editor **/
        forwardEvent = function(eventSource, eventName) {
          eventSource.on(eventName, function() { self.fireEvent(eventName, eventSource); });
        },

        initTranscriptionSections = function(transcriptionBodies) {

          var addEmptyField = function() {
                // Create empty transcription field
                var field = new TextEntryField(transcriptionSectionEl, {
                  placeholder: 'Transcribe...',
                  cssClass: 'new-transcription',
                  bodyType: 'TRANSCRIPTION'
                });

                forwardEvent(field, 'submit'); // Submit event needs to be handled by editor

                // If the user added a transcription, add it on commit
                queuedUpdates.push(function() {
                  if (!field.isEmpty())
                    currentAnnotation.bodies.push(field.getBody());
                });
              },

              addExisting = function() {
                jQuery.each(transcriptionBodies, function(idx, body) {
                  var transcriptionSection = new TranscriptionSection(transcriptionSectionEl, body);
                  handleDelete(transcriptionSection);
                  forwardEvent(transcriptionSection, 'submit'); // Submit event needs to be handled by editor
                  sections.push(transcriptionSection);
                });
              };

          if (transcriptionBodies.length === 0)
            // No transcription - adds empty field
            addEmptyField();
          else
            // Existing transcriptions - add list
            addExisting();
        },

        /** Common code for initializing a place section **/
        initPlaceSection = function(placeBody, toponym) {
          var placeSection = new PlaceSection(centerSectionEl, placeBody, toponym);
          forwardEvent(placeSection, 'change'); // Georesolution change needs to be handled by editor
          handleDelete(placeSection);
          sections.push(placeSection);
        },

        /** Common code for initializing a comment section **/
        initCommentSection = function(commentBody) {
          var commentSection = new CommentSection(centerSectionEl, commentBody);
          forwardEvent(commentSection, 'submit'); // Submit event needs to be handled by editor
          handleDelete(commentSection);
          sections.push(commentSection);
        },

        /** Creates a new section and queues creating in the annotation for later **/
        createNewSection = function(body, quote) {
          if (body.type === 'PLACE') {
            initPlaceSection(body, quote);
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

        /** Returns true if any of the sections was changed **/
        hasChanged = function() {
          var sectionsChanged = !sections.every(function(section) {
            return !section.hasChanged();
          });
          return sectionsChanged || queuedUpdates.length > 0;
        },

        /** Destroys all sections **/
        clear = function() {
          currentAnnotation = false;

          jQuery.each(sections, function(idx, section) {
            section.destroy();
          });

          transcriptionSectionEl.empty();

          sections = [];
          queuedUpdates = [];
        };

    this.setAnnotation = setAnnotation;
    this.createNewSection = createNewSection;
    this.updateSection = updateSection;
    this.hasChanged = hasChanged;
    this.commitChanges = commitChanges;
    this.clear = clear;

    HasEvents.apply(this);
  };
  SectionList.prototype = Object.create(HasEvents.prototype);

  return SectionList;

});
