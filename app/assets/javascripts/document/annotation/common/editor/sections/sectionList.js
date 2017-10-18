define([
  'document/annotation/common/editor/sections/comment/commentSection',
  'document/annotation/common/editor/sections/event/eventSection',
  'document/annotation/common/editor/sections/person/personSection',
  'document/annotation/common/editor/sections/place/placeSection',
  'document/annotation/common/editor/sections/tag/tagSection',
  'document/annotation/common/editor/sections/transcription/transcriptionSection',
  'document/annotation/common/editor/textEntryField',
  'common/utils/annotationUtils',
  'common/config',
  'common/hasEvents'
], function(
  CommentSection,
  EventSection,
  PersonSection,
  PlaceSection,
  TagSection,
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
        tagSectionEl = editorEl.find('.tag-section'),

        sections = [],

        queuedUpdates = [],

        newTranscriptionField = false,

        currentAnnotation = false,

        /** Initializes the section list from the annotation **/
        setAnnotation = function(annotation) {
          var transcriptionBodies = AnnotationUtils.getBodiesOfType(annotation, 'TRANSCRIPTION'),
              placeBodies = AnnotationUtils.getBodiesOfType(annotation, 'PLACE'),
              personBodies = AnnotationUtils.getBodiesOfType(annotation, 'PERSON'),
              eventBodies = AnnotationUtils.getBodiesOfType(annotation, 'EVENT'),
              commentBodies = AnnotationUtils.getBodiesOfType(annotation, 'COMMENT'),
              tagBodies = AnnotationUtils.getBodiesOfType(annotation, 'TAG');

          clear();

          currentAnnotation = annotation;

          // Transcriptions exist only on image content
          if (Config.contentType.indexOf('IMAGE') === 0)
            initTranscriptionSections(transcriptionBodies);

          jQuery.each(placeBodies, function(idx, placeBody) {
            initPlaceSection(placeBody); });

          jQuery.each(personBodies, function(idx, personBody) {
            initPersonSection(personBody); });

          jQuery.each(eventBodies, function(idx, eventBody) {
            initEventSection(eventBody); });

          jQuery.each(commentBodies, function(idx, commentBody) {
            initCommentSection(commentBody); });

          initTagSection(annotation);
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
              // Adds an empty 'Transcribe...' text entry field
          var initNewTranscriptionField = function() {
                newTranscriptionField = new TextEntryField(transcriptionSectionEl, {
                  placeholder: 'Transcribe...',
                  cssClass: 'new-transcription',
                  bodyType: 'TRANSCRIPTION'
                });

                forwardEvent(newTranscriptionField, 'submit'); // Submit event needs to be handled by editor

                // Make sure the field gets checked for existing text on commit
                queuedUpdates.push(function() {
                  if (!newTranscriptionField.isEmpty())
                    currentAnnotation.bodies.push(newTranscriptionField.getBody());
                });

                if (Config.hasFeature('multitranscription')) {
                  newTranscriptionField.on('enter', function() {
                    var body = newTranscriptionField.getBody();
                    addExistingTranscription(body);
                    queuedUpdates.push(function() {
                        currentAnnotation.bodies.push(body);
                    });
                    newTranscriptionField.clear();
                  });
                } else {
                  // Field hidden by default if other transcriptions exist already, or when read-only
                  if (transcriptionBodies.length > 0 || !Config.writeAccess)
                    newTranscriptionField.hide();
                }
              },

              addExistingTranscription = function(body) {
                var transcriptionSection = new TranscriptionSection(transcriptionSectionEl, body);
                handleDelete(transcriptionSection);
                forwardEvent(transcriptionSection, 'submit'); // Submit event needs to be handled by editor
                sections.push(transcriptionSection);
              };

          jQuery.each(transcriptionBodies, function(idx, body) { addExistingTranscription(body); });
          initNewTranscriptionField();
        },

        updateTranscriptionSections = function() {
          if (newTranscriptionField) {
            var transcriptionSections = jQuery.grep(sections, function(section) {
              return section instanceof TranscriptionSection;
            });

            if (transcriptionSections.length === 0)
              newTranscriptionField.show();
          }
        },

        /**
         * Returns the most recent transcription, including the one in the
         * newTranscription field (which isn't part of the annotation yet)
         */
        getMostRecentTranscription = function() {
          var storedTranscriptions = AnnotationUtils.getTranscriptions(currentAnnotation),
              newTranscription = (newTranscriptionField) ? newTranscriptionField.getBody() : false;

          if (newTranscription && newTranscription.value.trim().length > 0)
            // Field exists and contains a non-empty value
            return newTranscription.value.trim();
          else if (storedTranscriptions.length > 0)
            return storedTranscriptions[storedTranscriptions.length - 1];
        },

        initPlaceSection = function(placeBody, opt_toponym) {
          var placeSection = new PlaceSection(centerSectionEl, placeBody, opt_toponym);
          forwardEvent(placeSection, 'change'); // Georesolution change needs to be handled by editor
          handleDelete(placeSection);
          sections.push(placeSection);
        },

        initPersonSection = function(personBody) {
          var personSection = new PersonSection(centerSectionEl, personBody);
          handleDelete(personSection);
          sections.push(personSection);
        },

        initEventSection = function(eventBody) {
          var eventSection = new EventSection(centerSectionEl, eventBody);
          handleDelete(eventSection);
          sections.push(eventSection);
        },

        initCommentSection = function(commentBody) {
          var commentSection = new CommentSection(centerSectionEl, commentBody);
          forwardEvent(commentSection, 'submit'); // Submit event needs to be handled by editor
          handleDelete(commentSection);
          sections.push(commentSection);
        },

        initTagSection = function(annotation) {
          var tagSection = new TagSection(tagSectionEl, annotation);
          sections.push(tagSection);
        },

        /** Creates a new section and queues creating in the annotation for later **/
        createNewSection = function(body, quote) {
          if (body.type === 'PLACE') {
            initPlaceSection(body, quote);
          } else if (body.type === 'PERSON') {
            initPersonSection(body);
          } else if (body.type === 'EVENT') {
            initEventSection(body);
          }

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

          // Special case: if the destroyed section was the last transcription section,
          // we want to un-hide the 'new transcription' text entry field
          updateTranscriptionSections();

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
    this.getMostRecentTranscription = getMostRecentTranscription;

    HasEvents.apply(this);
  };
  SectionList.prototype = Object.create(HasEvents.prototype);

  return SectionList;

});
