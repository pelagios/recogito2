/**
 * A base class that encapsulates the common features of comment and transcription sections.
 */
define([
  'i18n!common/i18n/document/annotation/nls/editor',
  'common/config',
  'common/ui/behavior',
  'document/annotation/common/editor/sections/section'
], function(I18N, Config, Behavior, Section) {

  var EditableTextSection = function(element, annotationBody, opt_menuitems) {
    var self = this,

        textEntryDiv = element.find('.text'),
        lastModified = element.find('.last-modified'),

        btnOpenDropdown = element.find('.edit'),

        dropdownMenu = jQuery(
          '<ul class="edit-dropdown">' +
            '<li data-fn="edit">' + I18N.dropdown_edit + '</li>' +
            '<li data-fn="delete">' + I18N.dropdown_delete + '</li>' +
          '</ul>'),

        toggleEditDropdown = function(e) {
          if (e.target === btnOpenDropdown[0]) {
            // Click on this button - toggle
            if (btnOpenDropdown.hasClass('focus')) {
              btnOpenDropdown.removeClass('focus');
              dropdownMenu.hide();
            } else {
              btnOpenDropdown.addClass('focus');
              dropdownMenu.show();
            }
          } else {
            // Click anywhere else - close
            btnOpenDropdown.removeClass('focus');
            dropdownMenu.hide();
          }
        },

        enableEditDropdown = function() {
          // Append the menu element
          dropdownMenu.hide();
          element.append(dropdownMenu);

          // Need to handle this as global event, so we can:
          // - close when user clicks outside
          // - have radio-button like behaviour for multiple comments
          jQuery(document).on('click', toggleEditDropdown);

          // Make menu items clickable
          dropdownMenu.on('click', 'li', function(e) {
            var fn = jQuery(e.target).data('fn');
            if (fn === 'delete')
              self.fireEvent('delete');
            else if (fn === 'edit')
              self.makeEditable();
          });

          // To place the drop-down menu on top of the comment fields, we need to apply
          // a z-index in the inverse order: the first comment gets z-index 9999, second
          // 9998 a.s.o.
          element.css('z-index', 10000 - element.index());
        };

    if (Config.isAdmin || (Config.writeAccess && annotationBody.last_modified_by === Config.me))
      enableEditDropdown();
    else
      element.find('.edit').hide();

    textEntryDiv.html(self.escapeHtml(annotationBody.value));

    if (opt_menuitems)
      opt_menuitems.forEach(function(m) {
        var li = jQuery('<li>' + m.label + '</li>');
        li.click(m.fn);
        dropdownMenu.append(li);
      });

    this.body = annotationBody;

    this.element = element;
    this.textEntryDiv = textEntryDiv;
    this.lastModified = lastModified;

    this.toggleEditDropdown = toggleEditDropdown;

    Section.apply(this);
  };
  EditableTextSection.prototype = Object.create(Section.prototype);

  /** TODO we may want to allow HTML later - but then need to sanitize **/
  EditableTextSection.prototype.escapeHtml = function(text) {
    return jQuery('<div/>').text(text).html();
  };

  EditableTextSection.prototype.makeEditable = function() {
    var self = this;

    this.element.addClass('editing');
    this.textEntryDiv.prop('contenteditable', true);
    Behavior.placeCaretAtEnd(this.textEntryDiv[0]);
    this.lastModified.remove();

    this.textEntryDiv.keyup(function(e) {
      if (e.ctrlKey && e.keyCode == 13)
        self.fireEvent('submit');
    });
  };

  EditableTextSection.prototype.hasChanged = function() {
    var initialContent = this.body.value,
        currentContent = this.textEntryDiv.text().trim();

    return initialContent !== currentContent;
  };

  EditableTextSection.prototype.commit = function() {
    if (this.hasChanged()) {
      delete this.body.last_modified_at;
      this.body.last_modified_by = Config.me;
      this.body.value = this.textEntryDiv.text().trim();
    }
  };

  EditableTextSection.prototype.destroy = function() {
    jQuery(document).off('click', this.toggleEditDropdown);
    this.element.remove();
  };

  return EditableTextSection;

});
