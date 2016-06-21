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
    var permissionSelector = jQuery(
          '<div class="permission-selector">' +
            '<h3>Permission Level</h3>' +
            '<ul>' +
              '<li>' +
                '<h4>Admin</h4>' +
                '<p>Collaborators can edit document metadata, invite other collaborators, backup and restore, and roll back the edit history.</p>' +
              '</li>' +
              '<li>' +
                '<h4>Write</h4>' +
                '<p>Collaborators can read document and annotations, create new annotations, and add comments.</p>' +
              '</li>' +
              '<li>' +
                '<h4>Read</h4>' +
                '<p>Collaborators can read the document and annotations.</p>' +
              '</li>' +
            '</ul>' +
          '</div>'),

        publicAccessCheckbox = jQuery('#public-access'),
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

        togglePermissions = function(e) {
          var button = jQuery(e.target).closest('button'),
              position = button.position();

          if (permissionSelector.is(':visible')) {
            permissionSelector.hide();
          } else {
            permissionSelector.css({
              top: position.top + button.height() + 14,
              left: position.left + 2
            });
            permissionSelector.show();
          }
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
            var collabHome = jsRoutes.controllers.my.MyRecogitoController.index(result.collaborator).url,
                row = '<tr data-username="' + result.collaborator + '">' +
                        '<td><a href="' + collabHome + '">' + result.collaborator + '</a></td>' +
                        '<td>' +
                          '<button class="permissions btn small">' + result.access_level + '<span class="icon">&#xf0dd;</span></button>' +
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
    permissionSelector.hide();

    // TODO append (and re-append) to corresponding TD
    jQuery('.share-collab').append(permissionSelector);

    collaboratorsTable.on('click', '.remove-collaborator', removeCollaborator);
    collaboratorsTable.on('click', '.permissions', togglePermissions);
    addCollaboratorForm.submit(addCollaborator);
  });

});
