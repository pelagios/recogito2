require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/config'
], function(Config) {

  var App = function() {
    var dataURL = jsRoutes.controllers.document.DocumentController
          .getDataTable(Config.documentId, Config.partSequenceNo).absoluteURL(),

        onLoadComplete = function(results) {
          var dataView = new Slick.Data.DataView(),

              options = {
                enableCellNavigation:true,
                enableColumnReorder:false,
                fullWidthRows:true,
                defaultColumnWidth:120,
                rowHeight:34,
                frozenColumn:1
              },

              // Default leading columns
              frozenColumns = [
                { id: '__annotation', name: '', field: '__annotation', width: 50 },
                { id: 'id', name: '#', field: 'id', width: 50, sortable: true }
              ],

              columns = jQuery.map(results.meta.fields, function(f) {
                return { id: f, name: f, field: f, sortable: true };
              }),

              grid = new Slick.Grid('#table', dataView, frozenColumns.concat(columns), options),

              data = jQuery.map(results.data, function(f, idx) {
                f.id = idx;
                return f;
              }),

              onRowCountChanged = function(e, args) {
                grid.updateRowCount();
                grid.render();
              },

              onRowsChanged = function(e, args) {
                grid.invalidateRows(args.rows);
                grid.render();
              },

              onSelectedRowsChanged = function(e, args) {
                console.log(args);
                /*
                rightClickMenu.hide();
                if (args.rows.length === 0) {
                  currentSelection = false;
                } else {
                  currentSelection = args.rows;
                  if (args.rows.length > 0) {
                    var place = self._grid.getDataItem(args.rows[0]);
                    self.fireEvent('selectionChanged', args, place);
                  }
                }*/
              },

              onSort = function(e, args) {
                var comparator = function(a, b) {
                      var fieldA = a[args.sortCol.field],
                          fieldB = b[args.sortCol.field];
                      return (fieldA == fieldB ? 0 : (fieldA > fieldB ? 1 : -1));
                    };

                dataView.sort(comparator, args.sortAsc);
              };

          grid.onSort.subscribe(onSort);
          grid.setSelectionModel(new Slick.RowSelectionModel());
          grid.onSelectedRowsChanged.subscribe(onSelectedRowsChanged);

          dataView.onRowCountChanged.subscribe(onRowCountChanged);
          dataView.onRowsChanged.subscribe(onRowsChanged);
          dataView.setItems(data);

          // $(window).resize(function() { self._grid.resizeCanvas(); });
        },

        onLoadError = function(error) {
          // TODO implement
        };

    Papa.parse(dataURL, {
      download : true,
      header   : true, // TODO can we make this configurable through extra table meta?
      error    : onLoadError,
      complete : onLoadComplete
    });
  };

  /** Start on page load **/
  jQuery(document).ready(function() { new App(); });

});
