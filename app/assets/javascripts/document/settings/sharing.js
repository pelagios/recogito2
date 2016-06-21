require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/config'], function(Config) {

  var foo = function() {
    return function(q, cb) {
      jQuery.getJSON('/document/foo/settings/collaborator/search?q=' + q, function(results) {
        cb(results);
      });
    };
  };

  jQuery(document).ready(function() {
    var publicAccessCheckbox = jQuery('#public-access'),
        publicAccessLink = jQuery("#public-link"),

        collaboratorsTable = jQuery('.collaborators'),
        addCollaboratorForm = jQuery('.add-collaborators form'),
        addCollaboratorInput = addCollaboratorForm.find('input'),

        initAutosuggest = function() {
          addCollaboratorInput.typeahead({
            hint: false,
            highlight: false,
            minLength: 3
          }, {
            source: function(query, syncCallback, asyncCallback) {
              jsRoutes.controllers.document.settings.SettingsController.searchUsers(Config.documentId, query)
                .ajax().done(asyncCallback);
            }
          });

          addCollaboratorInput.on('typeahead:selected', function(e) {
            addCollaboratorForm.submit();
          });
        },

        onTogglePublicAccess = function() {
          var checked = publicAccessCheckbox.is(':checked');
          publicAccessCheckbox.prop('disabled', true);
          jsRoutes.controllers.document.settings.SettingsController.setIsPublic(Config.documentId, checked)
            .ajax().done(function() {
              publicAccessCheckbox.prop('disabled', false);

              // TODO extra 'green checkmark' feedback a la GitHub

            });
        },

        addCollaborator = function(e) {
          // Convert form data to object
          var data = addCollaboratorForm.serializeArray().reduce(function(obj, item) {
            obj[item.name] = item.value;
            return obj;
          }, {});

          jsRoutes.controllers.document.settings.SettingsController.addCollaborator(Config.documentId).ajax({
            contentType: 'application/json',
            data: JSON.stringify(data)
          });

          // TODO feedback on success or failure

          e.preventDefault();
          return false;
        },

        removeCollaborator = function(e) {
          var username = jQuery(e.target).closest('tr').data('username');
          jsRoutes.controllers.document.settings.SettingsController.removeCollaborator(Config.documentId, username).ajax();

          // TODO feedback on success or failure

        };

    // Public access panel
    publicAccessCheckbox.change(onTogglePublicAccess);
    publicAccessLink.focus(function() { jQuery(this).select(); } );

    // Add collaborators panel
    initAutosuggest();
    collaboratorsTable.on('click', '.remove-collaborator', removeCollaborator);
    addCollaboratorForm.submit(addCollaborator);
  });

});
