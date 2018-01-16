require([], function(Formatting) {

  jQuery(document).ready(function() {
    var fields = [
          { name: "username", type: "text", title: "Username" },
          { name: "email", type: "text", title: "E-Mail" },
          { name: "real_name", type: "text", title: "Real Name" },
          { name: "member_since", type: "text", title: "Member Since" },
          { name: "quota", type: "number", title: "Quota" },
          { type: "control",  editButton: false }
        ],

        loadData = function(filter) {
          var offset = (filter.pageIndex - 1) * filter.pageSize;

          return jsRoutes.controllers.admin.users.UserAdminController.listUsers(
            offset, filter.pageSize, filter.sortField, filter.sortOrder
          ).ajax().then(function(response) {
            return {
              'data': response.items,
              'itemsCount': response.total
            };
          });
        },

        deleteUser = function(user) {
          console.log('Deleting user ' + user.username);
          jsRoutes.controllers.admin.users.UserAdminController.deleteAccount(user.username)
            .ajax().done(function() {
              console.log('Done.');
            });
        };

    jQuery('.user-list').jsGrid({
      width:'100%',
      sorting: true,
      paging: true,
      pageSize: 40,
      pageLoading: true,
      autoload: true,
      fields: fields,
      controller: {
        loadData: loadData,
        deleteItem: deleteUser
      },
    });

  });

});
