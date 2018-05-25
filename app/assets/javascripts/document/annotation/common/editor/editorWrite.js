/**
 *
 * TODO there is now a hardcoded dependency between the editor and text selection functionality.
 * We'll need to break this dependency up in order to re-use the editor in image annotation mode.
 *
 */
define([
  'i18n!common/i18n/document/nls/annotation',
  'common/utils/annotationUtils',
  'common/utils/placeUtils',
  'common/api',
  'document/annotation/common/editor/editorBase',
  'document/annotation/common/editor/textEntryField',
  'document/annotation/common/georesolution/georesolutionPanel'
], function(I18N, AnnotationUtils, PlaceUtils, API, EditorBase, TextEntryField, GeoresolutionPanel) {

  var WriteEditor = function(container, annotations, selectionHandler, options) {
    var self = this,

        element = (function() {
          var el = jQuery(
                '<div class="annotation-editor-popup">' +
                  '<div class="arrow"></div>' +
                  '<div class="annotation-editor-popup-inner">' +
                    '<div class="transcription-sections"></div>' +
                    '<div class="category-buttons">' +
                      '<div class="category-container">' +
                        '<div class="category place">' +
                          '<span class="icon">&#xf041;</span> ' + I18N.editor_label_place +
                        '</div>' +
                      '</div>' +
                      '<div class="category-container">' +
                        '<div class="category person">' +
                          '<span class="icon">&#xf007;</span> ' + I18N.editor_label_person +
                        '</div>' +
                      '</div>' +
                      '<div class="category-container">' +
                        '<div class="category event">' +
                          '<span class="icon">&#xf005;</span> ' + I18N.editor_label_event +
                        '</div>' +
                      '</div>' +
                    '</div>' +
                    '<div class="center-sections"></div>' +
                    '<div class="reply-section"></div>' +
                    '<div class="tag-section"></div>' +
                    '<div class="footer">' +
                      '<button class="btn small outline cancel">' + I18N.btn_cancel + '</button>' +
                      '<button class="btn small outline ok-next">' + I18N.btn_ok_and_next + '</button>' +
                      '<button class="btn small ok">' + I18N.btn_ok + '</button>' +
                    '</div>' +
                  '</div>' +
                '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        btnPlace = element.find('.category.place'),
        btnPerson = element.find('.category.person'),
        btnEvent = element.find('.category.event'),

        btnCancel = element.find('button.cancel'),
        btnOkAndNext = element.find('button.ok-next'),
        btnOk = element.find('button.ok'),

        replyField = new TextEntryField(element.find('.reply-section'), {
          placeholder : I18N.editor_placeholder_add_comment,
          cssClass    : 'reply',
          bodyType    : 'COMMENT'
        }),

        georesolutionPanel = new GeoresolutionPanel(),

        annotationMode = { mode: 'NORMAL' },

        setAnnotationMode = function(mode) {
          annotationMode = mode;
        },

        openSelection = function(selection) {
          // In case of selection === undefined, close the editor
          if (!selection) {
            self.close();
            return;
          }

          if (selection.isNew) {
            // Branch based on annotation mode
            // TODO can we move dependency on mode outside the editor?
            if (annotationMode.mode === 'NORMAL') {
              self.open(selection);
            } else if (annotationMode.mode === 'QUICK') {
              // Quick modes just add an annotation body and trigger instant OK
              self.currentSelection = selection;

              if (annotationMode.type === 'PLACE') {
                API.searchPlaces(AnnotationUtils.getQuote(selection.annotation)).done(function(response) {
                  var quote = AnnotationUtils.getQuote(selection.annotation),
                      body = { type: 'PLACE', status: { value: 'UNVERIFIED' } };

                  if (response.total > 0)
                    body.uri = PlaceUtils.getBestMatchingRecord(response.items[0], quote).uri;

                  selection.annotation.bodies.push(body);
                  onOK();
                });
              } else if (annotationMode.type === 'PERSON') {
                selection.annotation.bodies.push({ type: 'PERSON' });
                onOK();
              }
            } else if (annotationMode.mode === 'BULK') {
              // TODO implement
            }
          } else {
            self.open(selection);
          }

          return false;
        },

        /** Click on 'Place' button adds a new PLACE body **/
        onAddPlace = function() {
          self.sectionList.createNewSection({ type: 'PLACE', status: { value: 'UNVERIFIED' } },
            self.getMostRecentContext());
        },

        /** Click on 'Person' button adds a new PERSON body **/
        onAddPerson = function() {
          self.sectionList.createNewSection({ type: 'PERSON' });
        },

        /** Click on 'Event' button adds a new EVENT body **/
        onAddEvent = function() {
          self.sectionList.createNewSection({ type: 'EVENT' });
        },

        /** 'Cancel' clears the selection and closes the editor **/
        onCancel = function() {
          selectionHandler.clearSelection();
          self.close();
        },

        /** 'OK' updates the annotation & highlight spans and closes the editor **/
        onOK = function() {
          var reply = replyField.getBody(), selection,
              currentAnnotation = self.currentSelection.annotation,
              hasChanged = self.updatedAnchor || reply || self.sectionList.hasChanged();

          if (hasChanged || annotationMode.mode === 'QUICK') {
            // Commit anchor changes
            if (self.updatedAnchor)
              self.currentSelection.annotation.anchor = self.updatedAnchor;

            // Commit changes in sections
            self.sectionList.commitChanges();

            // Push the current reply body, if any
            if (reply)
              currentAnnotation.bodies.push(reply);

            // Determine which CRUD action to perform
            if (currentAnnotation.annotation_id) {
              // There's an ID - annotation already stored on the server
              if (AnnotationUtils.isEmpty(currentAnnotation))
                // Annotation empty - DELETE
                self.fireEvent('deleteAnnotation', currentAnnotation);
              else
                // UPDATE
                self.fireEvent('updateAnnotation', currentAnnotation);
            } else {
              // No ID? New annotation from fresh selection - CREATE if not empty
              if (AnnotationUtils.isEmpty(currentAnnotation))
                selectionHandler.clearSelection();
              else
                self.fireEvent('createAnnotation', self.currentSelection);
            }
          } else {
            selectionHandler.clearSelection();
          }

          self.close();
        },

        /** Shortcut: triggers OK and moves the editor to the next annotation **/
        onOKAndNext = function() {
          onOK();
        },

        /** User clicked 'change georesolution' - open the panel **/
        onChangeGeoresolution = function(section) {
          georesolutionPanel.open(self.getMostRecentContext(), section.body);
        },

        /** Georesolution was changed - forward changes to the section list **/
        onGeoresolutionChanged = function(placeBody, diff) {
          self.sectionList.updateSection(placeBody, diff);
        };

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

    this.openSelection = openSelection;
    this.setAnnotationMode = setAnnotationMode;

    this.replyField = replyField;

    EditorBase.apply(this, [ container, element, annotations, options ]);

    // Events from the section List
    this.sectionList.on('submit', onOK);
    this.sectionList.on('change', onChangeGeoresolution);

    // ESC key doubles as 'Cancel'
    this.on('escape', onCancel);

    // TODO handle click on background document -> cancel
  };
  WriteEditor.prototype = Object.create(EditorBase.prototype);

  EditorBase.prototype.updateAnchor = function(selection) {
    if (selection) {
      this.updatedAnchor = selection.annotation.anchor;
      this.setPosition(selection.bounds);
    }
  };

  /** Extends the clear method provided by EditorBase **/
  WriteEditor.prototype.clear = function() {
    this.replyField.clear();
    this.updatedAnchor = undefined;
    EditorBase.prototype.clear.call(this);
  };

  /** Extends the open method provided by EditorBase **/
  WriteEditor.prototype.open = function(selection) {
    EditorBase.prototype.open.call(this, selection);

    if (AnnotationUtils.countComments(selection.annotation) > 0)
      this.replyField.setPlaceHolder('Write a reply...');

    return false;
  };

  return WriteEditor;

});
