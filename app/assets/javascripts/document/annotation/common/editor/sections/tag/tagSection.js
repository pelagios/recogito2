define([
  'common/config',
  'document/annotation/common/editor/sections/section'
], function(Config, Section) {

  var TagSection = function(parent, annotation) {
    var element = jQuery(
          '<div class="section tags">' +
            '<ul></ul>' +
            '<div contenteditable="true" spellcheck="false" class="add-tag" data-placeholder="Add tag..." />' +
          '</div>'),

        taglist = element.find('ul'),
        textarea = element.find('.add-tag'),

        queuedUpdates = [],

        init = function() {
          jQuery.each(annotation.bodies, function(idx, body) {
            if (body.type === 'TAG')
              addTag(body.value);
          });
        },

        hasChanged = function() {
          return queuedUpdates.length > 0;
        },

        commit = function() {
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });
        },

        destroy = function() {
          element.remove();
        },

        addTag = function(chars) {
          queuedUpdates.push(function() {
            annotation.bodies.push({
              type: 'TAG', last_modified_by: Config.me, value: chars
            });
          });

          taglist.append('<li>' + chars + '</li>');
        },

        onKeyDown = function(e) {
          if (e.keyCode === 13) {
            addTag(textarea.text().trim());
            textarea.empty();
            textarea.blur();
            return false;
          }
        };

    init();
    textarea.keydown(onKeyDown);
    parent.append(element);

    this.hasChanged = hasChanged;
    this.commit = commit;
    this.destroy = destroy;
    this.body = {}; // N/A

    Section.apply(this);
  };
  TagSection.prototype = Object.create(Section.prototype);

  return TagSection;

});
