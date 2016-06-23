require([], function(Config) {

  jQuery(document).ready(function() {
        /** NEXT button to skip to next wizard phase **/
    var nextButton = jQuery('button[type=submit]'),

        /** Mandatory title field - disable NEXT button unless filled! **/
        titleField = jQuery('input[name=title]'),

        /** Toggle extra metadata fields **/
        toggleButton = jQuery('.field-toggle'),
        toggleIcon = toggleButton.find('.icon'),
        toggleLabel = toggleButton.find('.toggle-label'),
        extraFields = jQuery('.more-fields'),

        onEditTitle = function() {
          if (titleField.val().trim().length === 0)
            nextButton.prop('disabled', true);
          else
            nextButton.prop('disabled', false);
        },

        onToggle = function() {
          if (extraFields.is(':visible')) {
            toggleIcon.html('&#xf078;');
            toggleLabel.html('More Fields');
          } else {
            toggleIcon.html('&#xf077;');
            toggleLabel.html('Hide Extra Fields');
          }

          extraFields.slideToggle(200);
        };

    /** Set NEXT button initial state... **/
    onEditTitle();

    /** ... and listen to key events on mandatory title field **/
    titleField.keyup(onEditTitle);

    /** Enable toggling **/
    toggleButton.click(onToggle);
  });

});
