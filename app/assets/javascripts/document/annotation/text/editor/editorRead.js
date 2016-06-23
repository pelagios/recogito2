
define(['document/annotation/text/editor/editorBase'], function(EditorBase) {

  var ReadEditor = function(container) {
    var self = this,

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

        btnClose = element.find('button.close'),

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

    // Editor closes on button and ESC key
    btnClose.click(onClose);
    self.on('escape', onClose);
  };
  ReadEditor.prototype = Object.create(EditorBase.prototype);

  return ReadEditor;

});
