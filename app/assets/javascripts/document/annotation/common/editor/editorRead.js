
define(['document/annotation/common/editor/editorBase'], function(EditorBase) {

  var ReadEditor = function(container) {
    var self = this,

        element = (function() {
          var el = jQuery(
                '<div class="annotation-editor-popup readonly">' +
                  '<div class="arrow"></div>' +
                  '<div class="annotation-editor-popup-inner">' +
                    '<div class="transcription-sections"></div>' +
                    '<div class="center-sections"></div>' +
                  '</div>' +
                '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        openSelection = function(selection) {
          if (selection)
            self.open(selection);
          else
            self.close();
          return false;
        };

    this.openSelection = openSelection;

    EditorBase.apply(this, [ container, element ]);

    // Editor closes on ESC key
    self.on('escape', self.close.bind(self));
  };
  ReadEditor.prototype = Object.create(EditorBase.prototype);

  return ReadEditor;

});
