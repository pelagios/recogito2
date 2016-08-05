define(['common/config', 'common/hasEvents'], function(Config, HasEvents) {

  var ReplyField = function(parent, isReply) {
    var self = this,

        element = jQuery(
          '<div class="field reply">' +
            '<div contenteditable="true" spellcheck="false" class="textarea" data-placeholder="Add a comment..." />' +
          '</div>'),

        textarea = element.find('.textarea'),

        clear = function() {
          textarea.empty();
          setPlaceHolder();
        },

        setPlaceHolder = function(placeholder) {
          if (placeholder)
            textarea.attr('data-placeholder', placeholder);
          else
            textarea.attr('data-placeholder', 'Add a comment...');
        },

        getComment = function() {
          var val = textarea.text().trim();
          if (val)
            return { type: 'COMMENT', last_modified_by: Config.me, value: val };
          else
            return false;
        },

        onKeyUp = function(e) {
          if (e.ctrlKey && e.keyCode == 13)
            self.fireEvent('submit', getComment());
        };

    textarea.keyup(onKeyUp);
    parent.append(element);

    this.clear = clear;
    this.getComment = getComment;
    this.setPlaceHolder = setPlaceHolder;

    HasEvents.apply(this);
  };
  ReplyField.prototype = Object.create(HasEvents.prototype);

  return ReplyField;

});
