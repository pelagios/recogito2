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
            '<h3>Permission Level<span class="close outline-icon">&#xe897;</close></h3>' +
            '<ul>' +
              '<li class="READ">' +
                '<div class="checked"><div class="icon"></div></div>' +
                '<h4>Read</h4>' +
                '<p>Collaborators can read document and annotations, but not edit.</p>' +
              '</li>' +
              '<li class="WRITE">' +
                '<div class="checked"><div class="icon"></div></div>' +
                '<h4>Write</h4>' +
                '<p>Collaborators can read document and annotations, create new annotations, and add comments.</p>' +
              '</li>' +
              '<li class="ADMIN">' +
                '<div class="checked"><div class="icon"></div></div>' +
                '<h4>Admin</h4>' +
                '<p>Admins can edit document metadata, invite other collaborators, backup and restore, and roll back the edit history.</p>' +
              '</li>' +
            '</ul>' +
          '</div>'),

        publicAccessCheckbox = jQuery('#public-access'),
        publicAccessLink = jQuery("#public-link"),

        noCollaboratorsMessage = jQuery('.no-collaborators'),
        collaboratorsTable = jQuery('.collaborators'),
        addCollaboratorForm = jQuery('.add-collaborators form'),
        addCollaboratorInput = addCollaboratorForm.find('input'),

        closePermissionButton = permissionSelector.find('.close'),

        currentCollaborator = false,

        initAutosuggest = function() {
          addCollaboratorInput.typeahead({
            hint: false,
            highlight: false,
            minLength: 3
          }, {
            source: function(query, syncCallback, asyncCallback) {
              // Remove current user and users that are already collaborators
              var toRemove = getCollaborators();
              toRemove.push(Config.me);

              jsRoutes.controllers.document.settings.SettingsController.searchUsers(Config.documentId, query)
                .ajax().done(function(results) {
                  var filtered = jQuery.grep(results, function(username) {
                    return toRemove.indexOf(username) == -1;
                  });
                  asyncCallback(filtered);
                });
            }
          });

          addCollaboratorInput.on('typeahead:selected', function(e) {
            addCollaboratorForm.submit();
          });
        },

        getCollaborators = function() {
          return jQuery.map(collaboratorsTable.find('tr'), function(tr) {
            return tr.dataset.username;
          });
        },

        getCollaboratorForRow = function(tr) {
          return {
            collaborator: username = tr.data('username'),
            access_level: tr.data('level')
          };
        },

        getRowForCollaborator = function(username) {
          var rows = jQuery.grep(collaboratorsTable.find('tr'), function(tr) {
            return jQuery(tr).data('username') === username;
          });

          if (rows.length > 0)
            return jQuery(rows[0]);
          else
            return false;
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
              collaborator = getCollaboratorForRow(button.closest('tr')),
              position = button.position();

          if (permissionSelector.is(':visible')) {
            closePermissions();
          } else {
            // Remember collaborator for later
            currentCollaborator = collaborator;

            // Set checkmark
            permissionSelector.find('.icon').empty();
            permissionSelector.find('.' + collaborator.access_level + ' .icon').html('&#xf00c;');

            // Set position
            permissionSelector.css({
              top: position.top + button.height() + 14,
              left: position.left + 2
            });
            permissionSelector.show();
          }
        },

        closePermissions = function() {
          permissionSelector.hide();
        },

        clearCollaboratorInput = function() {
          addCollaboratorInput.blur();
          addCollaboratorInput.val('');
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
            if (result.new_collaborator) {
              var collabHome = jsRoutes.controllers.my.MyRecogitoController.index(result.collaborator).url,
                  row = '<tr data-username="' + result.collaborator + '" data-level="' + result.access_level + '">' +
                          '<td><a href="' + collabHome + '">' + result.collaborator + '</a></td>' +
                          '<td>' +
                            '<button class="permissions btn small">' +
                              '<span class="label">' + result.access_level + '</span>' +
                              '<span class="icon">&#xf0dd;</span>' +
                            '</button>' +
                          '</td>' +
                          '<td class="outline-icon remove-collaborator">&#xe897;</td>' +
                        '</tr>';

              closePermissions();
              collaboratorsTable.append(row);
              clearCollaboratorInput();
            }
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

        },

        changePermissionLevel = function(e) {
          var level = jQuery(e.target).closest('li').attr('class');
          if (currentCollaborator && currentCollaborator.access_level != level) {
            jsRoutes.controllers.document.settings.SettingsController.addCollaborator(Config.documentId).ajax({
              contentType: 'application/json',
              data: JSON.stringify({ collaborator: currentCollaborator.collaborator, access_level: level })
            }).done(function(result) {
              var row = getRowForCollaborator(result.collaborator),
                  button = row.find('.permissions .label');

              currentCollaborator = false;
              row.data('level', result.access_level);
              button.html(result.access_level);
              permissionSelector.hide();
            });
          }
        };

    // Public access panel
    publicAccessCheckbox.change(onTogglePublicAccess);
    publicAccessLink.focus(function() { jQuery(this).select(); } );

    // Add collaborators panel
    initAutosuggest();
    permissionSelector.hide();
    permissionSelector.on('click', 'li', changePermissionLevel);

    closePermissionButton.click(closePermissions);

    // TODO append (and re-append) to corresponding TD
    jQuery('.share-collab').append(permissionSelector);

    collaboratorsTable.on('click', '.remove-collaborator', removeCollaborator);
    collaboratorsTable.on('click', '.permissions', togglePermissions);
    addCollaboratorForm.submit(addCollaborator);
  });

});
