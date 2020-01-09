define([
  'i18n!common/i18n/document/nls/annotation',
  'common/config',
  'document/annotation/common/editor/sections/section',
  'document/annotation/common/editor/sections/tag/tagAutocomplete'
], function(I18N, Config, Section, TagAutocomplete) {

  var DELETE_WIDTH = 23,

      ANIM_DURATION = 150;

  var TagSection = function(parent, annotation, allAnnotationsOnPage) {

    var element = (Config.writeAccess) ? jQuery(
          '<div class="section tags">' +
            '<ul></ul>' +
            '<input type="text" class="add-tag" placeholder="' + I18N.editor_placeholder_add_tag + '"></input>' +
          '</div>') : jQuery('<div class="section tags readonly"><ul></ul></div>'),

        taglist = element.find('ul'),
        textarea = element.find('.add-tag'),

        autocomplete = new TagAutocomplete(element, textarea, allAnnotationsOnPage),

        queuedUpdates = [],

        /**
         * Creates a new tag element and attaches the tag to it as data.
         *
         * Takes either a tag object or a string as input.
         */
        createTag = function(charsOrTag) {
          var escapeHtml = function(text) {
                return jQuery('<div/>').text(text).html();
              },

              tag = (charsOrTag.type) ? charsOrTag :
                { type: 'TAG', last_modified_by: Config.me, value: charsOrTag.trim() },

              li = jQuery('<li><span class="label">' + escapeHtml(tag.value) + '</span>' +
                '<span class="delete"><span class="icon">&#xf014;</span></span></li>');

          li.data('tag', tag);
    
          // Make drag-sortable if write access
          if (Config.writeAccess) {
            li.draggable({ 
              connectToSortable: taglist,
              revert: 'invalid',
              revertDuration: 10
            });
          }

          return li;
        },

        /** Initializes the tag list from the annotation bodies **/
        init = function() {
          var tagCount = 0;
          jQuery.each(annotation.bodies, function(idx, body) {
            if (body.type === 'TAG') {
              taglist.append(createTag(body));
              tagCount++;
            }
          });

          // In read-only mode, hide the list if there are no tags
          if (!Config.writeAccess && tagCount === 0)
            element.hide();
        },

        /** Tests if the given character string exists as a tag already **/
        exists = function(chars) {
          var existing = jQuery.grep(taglist.children(), function(el) {
            var tagChars = jQuery(el).find('.label').text();
            return tagChars === chars;
          });
          return existing.length > 0;
        },

        /** Adds a new tag to the annotation **/
        addTag = function(chars) {
          if (!exists(chars)) {
            var li = createTag(chars),
                tag = li.data('tag');

            taglist.append(li);
            queuedUpdates.push(function() { annotation.bodies.push(tag); });
          }
        },

        /** Deletes a tag from the annotation **/
        deleteTag = function(li) {
          var tag = li.data('tag');
          li.remove();
          queuedUpdates.push(function() {
            var idx = annotation.bodies.indexOf(tag);
            if (idx > -1)
              annotation.bodies.splice(idx, 1);
          });
        },

        /** Shows the delete button on the given tag element **/
        showDeleteButton = function(li) {
          var delIcon = li.find('.delete');
          li.animate({ 'padding-right' : DELETE_WIDTH }, ANIM_DURATION);
          delIcon.animate({ 'width': DELETE_WIDTH }, ANIM_DURATION);
        },

        /** Hides all currently visible delete buttons **/
        hideAllDeleteButtons = function() {
          jQuery.each(taglist.find('li'), function(idx, el) {
            var li = jQuery(el),
                delIcon = li.find('.delete');
                isClicked = delIcon.width() > 0;

            if (isClicked) {
              li.animate({ 'padding-right' : 0 }, ANIM_DURATION);
              delIcon.animate({ 'width': 0 }, ANIM_DURATION);
            }
          });
        },

        /** Click toggles the delete button or deletes, depending on state & click target **/
        onTagClicked = function(e) {
          var isDelete = (e.target).closest('.delete'),
              li = jQuery(e.target).closest('li'),
              chars = li.find('.label').text();

          if (isDelete) {
            deleteTag(li);
          } else {
            showDeleteButton(li);
            hideAllDeleteButtons();
          }
        },

        /** Tags that are currently in 'draft' in the edit field **/
        getDraftTags = function() {
          var str = textarea.val().trim(); //.split(',');
          if (str)
            return str.split(',');
          else
            return [];
        },

        /** Text entry field: new tags are created on ENTER **/
        onKeyDown = function(e) {
          if (e.keyCode === 13) {
            var tags = getDraftTags();
            jQuery.each(tags, function(idx, chars) {
              addTag(chars.trim());
            });
            autocomplete.hide();
            textarea.val('');
            return false;
          }
        },

        /** @override **/
        hasChanged = function() {
          return queuedUpdates.length > 0 || getDraftTags().length > 0;
        },

        /** @override **/
        commit = function() {
          // Add draft tags to the queue as well
          getDraftTags().forEach(addTag);
          jQuery.each(queuedUpdates, function(idx, fn) { fn(); });
        },

        /** @override **/
        destroy = function() {
          element.remove();
        };

    init();

    if (Config.writeAccess) {
      taglist.on('click', 'li', onTagClicked);

      taglist.sortable({ revert: 10 });

      textarea.keydown(onKeyDown);
    }

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
