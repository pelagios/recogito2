
define(['document/annotation/text/editor/editorBase'], function(EditorBase) {

  var ReadEditor = function(container) {
    var self = this,

        element = (function() {
          var el = jQuery(
                '<div class="text-annotation-editor">' +
                  '<div class="arrow"></div>' +
                  '<div class="text-annotation-editor-inner">' +
                    '<div class="sections"></div>' +
                  '</div>' +
                '</div>');

          jQuery(container).append(el);
          el.hide();
          return el;
        })(),

        /** Opens the editor on an annotation **/
        viewAnnotation = function(e) {
          var element = e.target,
              allAnnotations = self.highlighter.getAnnotationsAt(element);

          self.open(allAnnotations[0], element.getBoundingClientRect());
          return false;
        },

        /** 'OK' updates the annotation & highlight spans and closes the editor **/
        onClose = function() {
          element.hide();
        };

    EditorBase.apply(this, [ container, element ]);

    // Click on annotation span opens the editor
    jQuery(container).on('click', '.annotation', viewAnnotation);


    // Editor closes on ESC key and click on background document
    self.on('escape', onClose);
    jQuery(document).on('click', ':not(> .text-annotation-editor)', onClose);
  };
  ReadEditor.prototype = Object.create(EditorBase.prototype);

  return ReadEditor;

});
