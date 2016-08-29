
define(['document/annotation/common/editor/editorBase'], function(EditorBase) {

  var ReadEditor = function(container, highlighter) {
    var self = this,

        element = (function() {
          var el = jQuery(
                '<div class="text-annotation-editor">' +
                  '<div class="arrow"></div>' +
                  '<div class="text-annotation-editor-inner">' +
                    '<div class="transcription-sections"></div>' +
                    '<div class="center-sections"></div>' +
                  '</div>' +
                '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        openSelection = function(selection) {
          self.open(selection.annotation, selection.bounds);
          return false;
        },

        /** 'OK' updates the annotation & highlight spans and closes the editor **/
        onClose = function() {
          element.hide();
        };

    this.openSelection = openSelection;

    EditorBase.apply(this, [ container, element, highlighter ]);

    // Editor closes on ESC key and click on background document
    self.on('escape', onClose);

    // jQuery(document).on('click', ':not(> .text-annotation-editor)', onClose);
  };
  ReadEditor.prototype = Object.create(EditorBase.prototype);

  return ReadEditor;

});
