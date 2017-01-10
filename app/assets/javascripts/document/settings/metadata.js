require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/modal',
  'common/config'
], function(Modal, Config) {

  var PartMetadataEditor = function(part) {
    var self = this,

       form = jQuery(
          '<form class="crud">' +
            '<div class="error"></div>' +
            '<dl id="part-title-field">' +
              '<dt><label for="part-title">Title</label></dt>' +
              '<dd>' +
                '<input type="text" id="part-title" name="part-title" value="' + part.title + '" autocomplete="false">' +
              '</dd>' +
            '</dl>' +

            '<dl id="part-source-field">' +
              '<dt><label for="part-source">Source</label></dt>' +
              '<dd>' +
                '<input type="text" id="part-source" name="part-source" autocomplete="false">' +
              '</dd>' +
            '</dl>' +

            '<dt></dt>' +
            '<button type="submit" class="btn">Save Changes</button>' +
          '</form>'),

        errorMessage = form.find('.error'),

        init = function() {
          // Populate form
          if (part.source)
            form.find('#part-source').attr('value', part.source);

          // Form submit handler
          form.submit(onSubmit);
        },

        getValue = function(selector) {
          var value = form.find(selector).val().trim();
          if (value.length > 0)
            return value;
        },

        onSubmit = function() {
          jsRoutes.controllers.document.settings.SettingsController.updateFilepartMetadata(Config.documentId, part.id).ajax({
            data: { title: getValue('#part-title'), source: getValue('#part-source') }
          }).success(function() {
            self.destroy();
            window.location.reload(true);
          }).fail(function(error) {
            errorMessage.html(error.responseText);
          });
          return false;
        };

    init();

    Modal.apply(this, [ 'Place Annotation', form ]);
  };
  PartMetadataEditor.prototype = Object.create(Modal.prototype);

  jQuery(document).ready(function() {

    var partList = jQuery('.part-metadata ul'),

        parts = jQuery('li.filepart'),

        flashMessage = jQuery('.part-metadata .flash-message'),

        clearFlashMessage = function() {
          flashMessage.hide();
        },

        setFlashMessage = function(cssClass, html) {
          flashMessage.removeClass();
          flashMessage.addClass(cssClass + ' flash-message');
          flashMessage.html(html);
          flashMessage.show();
        },

        onOrderChanged = function() {
          var sortOrder = jQuery.map(parts, function(li) {
                var part = jQuery(li);
                return { id: part.data('id'), sequence_no: part.index() + 1 };
              });

          jsRoutes.controllers.document.settings.SettingsController.setSortOrder(Config.documentId).ajax({
            data: JSON.stringify(sortOrder),
            contentType: 'application/json'
          }).success(function() {
            setFlashMessage('success', '<span class="icon">&#xf00c;</span> Your settings have been saved.');
          }).fail(function(error) {
            setFlashMessage('error', '<span class="icon">&#xf00d;</span> ' + error);
          });
        },

        onOpenPartEditor = function(e) {
          var partId = jQuery(e.target).closest('li').data('id'),

              part = jQuery.grep(Config.fileparts, function(part) {
                return part.id === partId;
              })[0],

              editor = new PartMetadataEditor(part);
        };

    // Make filepart elements sortable (and disable selection)
    partList.disableSelection();
    partList.sortable({
      start: clearFlashMessage,
      stop: onOrderChanged
    });

    // 'Edit part metadata' button handler
    partList.on('click', 'button', onOpenPartEditor);
  });

});
