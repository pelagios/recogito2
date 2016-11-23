define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'
], function(Config, AbstractSelectionHandler) {

  var SelectionHandler = function(grid, highlighter) {

    var self = this,

        dataView = grid.getData(),

        onSelectedRowsChanged = function(e, args) {
          if (args.rows.length > 0) {
            var firstRowIdx = args.rows[0], // TODO support multi-select

                firstRowData = dataView.getItem(firstRowIdx),

                firstRowElement = grid.getActiveCellNode().closest('.slick-row.active')[0],

                annotationStub = {
                  annotates: {
                    document_id: Config.documentId,
                    filepart_id: Config.partId,
                    content_type: Config.contentType
                  },
                  anchor: 'row:' + firstRowIdx,
                  bodies: [
                    { type: 'QUOTE', value: '' /* TODO 'quote cell' value */ }
                  ]
                },

                selection = {
                  isNew: true,
                  annotation: annotationStub,
                  bounds: firstRowElement.getBoundingClientRect()
                };

            self.fireEvent('select', selection);
          }
        },

        getSelection = function() {
          var rows = grid.getSelectedRows();
          console.log(rows);
        },

        setSelection = function(selection) {
          console.log(selection);
        },

        clearSelection = function() {
          grid.setSelectedRows([]);
        };

    grid.setSelectionModel(new Slick.RowSelectionModel());
    grid.onSelectedRowsChanged.subscribe(onSelectedRowsChanged);

    this.getSelection = getSelection;
    this.setSelection = setSelection;
    this.clearSelection = clearSelection;

    AbstractSelectionHandler.apply(this);
  };
  SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

  return SelectionHandler;

});
