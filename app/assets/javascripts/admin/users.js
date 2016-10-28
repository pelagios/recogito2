require([], function(Formatting) {

  jQuery(document).ready(function() {
    var fields = [
          { name: "username", type: "text", title: "Username" },
          { name: "email", type: "text", title: "E-Mail" },
          { name: "real_name", type: "text", title: "Real Name" },
          { name: "member_since", type: "text", title: "Member Since" },
          { name: "quota", type: "number", title: "Quota" }
        ],

        loadData = function(filter) {
          var offset = (filter.pageIndex - 1) * filter.pageSize;

          return jsRoutes.controllers.api.UserAPIController.listUsers(
            offset, filter.pageSize, filter.sortField, filter.sortOrder
          ).ajax().then(function(response) {
            return {
              'data': response.items,
              'itemsCount': response.total
            };
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
      controller: { loadData: loadData },
    });

  });

});
