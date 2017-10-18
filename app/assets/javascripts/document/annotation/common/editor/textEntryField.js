define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var TextEntryField = function(parent, options) {
    var self = this,

        element = jQuery(
          '<div class="field ' + options.cssClass + '">' +
            '<div contenteditable="true" spellcheck="false" class="textarea" data-placeholder="' + options.placeholder + '" />' +
          '</div>'),

        textarea = element.find('.textarea'),

        show = function() {
          element.show();
        },

        hide = function() {
          element.hide();
        },

        clear = function() {
          textarea.empty();
          setPlaceHolder();
        },

        isEmpty = function() {
          return textarea.text().trim().length === 0;
        },

        setPlaceHolder = function(placeholder) {
          if (placeholder)
            textarea.attr('data-placeholder', placeholder);
          else
            textarea.attr('data-placeholder', options.placeholder);
        },

        getBody = function() {
          var val = textarea.text().trim();
          if (val)
            return  { type: options.bodyType, last_modified_by: Config.me, value: val };
          else
            return false;
        },

        onKeyUp = function(e) {
          if (e.keyCode == 13) {
            self.fireEvent('enter');
            if (e.ctrlKey)
              self.fireEvent('submit');
          }
        };

    textarea.keyup(onKeyUp);
    parent.append(element);

    this.show = show;
    this.hide = hide;
    this.clear = clear;
    this.isEmpty = isEmpty;
    this.getBody = getBody;
    this.setPlaceHolder = setPlaceHolder;

    HasEvents.apply(this);
  };
  TextEntryField.prototype = Object.create(HasEvents.prototype);

  return TextEntryField;

});
