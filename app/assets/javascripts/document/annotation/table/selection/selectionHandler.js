define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'
], function(Config, AbstractSelectionHandler) {

  var SelectionHandler = function(grid, highlighter) {

    var self = this,

        dataView = grid.getData(),

        leftViewPort = jQuery('.slick-viewport-left'),

        getFrozenRowElement = function() {
          var selectedRow = grid.getActiveCellNode().closest('.slick-row.active'),
              isFrozenRow = selectedRow.parents('.slick-viewport-left').length > 0;

          // We always want the popup to hover over the frozen side of the row
          if (!isFrozenRow)
            selectedRow = leftViewPort.find('.slick-row').eq(selectedRow.index());

          return selectedRow.find('.l1')[0];
        },

        onSelectedRowsChanged = function(e, args) {

          var createNewSelection = function(rowIdx, rowElement) {
                var annotationStub = {
                      annotates: {
                        document_id: Config.documentId,
                        filepart_id: Config.partId,
                        content_type: Config.contentType
                      },
                      anchor: 'row:' + rowIdx,
                      bodies: [
                        { type: 'QUOTE', value: '' /* TODO 'quote cell' value */ }
                      ]
                    };

                return {
                  isNew: true,
                  annotation: annotationStub,
                  bounds: rowElement.getBoundingClientRect()
                };
              },

              getExisingSelection = function(rowElement, rowData) {
                return {
                  isNew: false,
                  annotation: rowData.__annotation,
                  bounds: rowElement.getBoundingClientRect()
                };
              };

          if (args.rows.length > 0) {

            // TODO support multi-select

            var firstRowIdx = args.rows[0],
                firstRowElement = getFrozenRowElement();
                firstRowData = dataView.getItem(firstRowIdx);

            if (firstRowData.__annotation)
              self.fireEvent('select', getExisingSelection(firstRowElement, firstRowData));
            else
              self.fireEvent('select', createNewSelection(firstRowIdx, firstRowElement));
          }
        },

        getSelection = function() {
          var rows = grid.getSelectedRows();
          console.log('get selection');
        },

        setSelection = function(selection) {
          console.log('set selection');
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
