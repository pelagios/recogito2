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

        noCollaboratorsMessage = jQuery('.no-collaborators'),
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
          }).done(function(result) {
            var row =
              '<tr data-username="' + result.collaborator + '">' +
                '<td>' + result.collaborator + '</td>' +
                '<td>' +
                  '<button class="btn small">' + result.access_level + '<span class="icon">&#xf0dd;</span></button>' +
                '</td>' +
                '<td class="outline-icon remove-collaborator">&#xe897;</td>' +

              '</tr>';

            noCollaboratorsMessage.hide();
            collaboratorsTable.append(row);
          });

          // TODO what in case of failure?

          e.preventDefault();
          return false;
        },

        removeCollaborator = function(e) {
          var username = jQuery(e.target).closest('tr').data('username');
          jsRoutes.controllers.document.settings.SettingsController.removeCollaborator(Config.documentId, username)
            .ajax().done(function() {
              var row = jQuery('tr[data-username="' + username + '"]');
              row.remove();

              if (collaboratorsTable.find('tr').length === 0)
                noCollaboratorsMessage.show();
            });

            // TODO what in case of failure?

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
