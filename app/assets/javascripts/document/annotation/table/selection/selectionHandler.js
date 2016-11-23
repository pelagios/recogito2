define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'
], function(Config, AbstractSelectionHandler) {

  var SelectionHandler = function(grid, highlighter) {

    var self = this,

        dataView = grid.getData(),

        leftViewPort = jQuery('.slick-viewport-left'),

        currentSelection = false,

        getFrozenRowElement = function() {
          var selectedRow = grid.getActiveCellNode().closest('.slick-row.active'),
              isFrozenRow = selectedRow.parents('.slick-viewport-left').length > 0;

          // We always want the popup to hover over the frozen side of the row
          if (!isFrozenRow)
            selectedRow = leftViewPort.find('.slick-row').eq(selectedRow.index());

          return selectedRow.find('.l1')[0];
        },

        onSelectedRowsChanged = function(e, args) {
          var createNewSelection = function(rowIdx, cellDiv) {
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
                  bounds: cellDiv.getBoundingClientRect(),
                  cell: cellDiv
                };
              },

              getExisingSelection = function(cellDiv, rowData) {
                return {
                  isNew: false,
                  annotation: rowData.__annotation,
                  bounds: cellDiv.getBoundingClientRect(),
                  cell: cellDiv
                };
              };

          if (args.rows.length > 0) {

            // TODO support multi-select

            var firstRowIdx = args.rows[0],
                firstRowElement = getFrozenRowElement(),
                firstRowData = dataView.getItem(firstRowIdx),
                selection = (firstRowData.__annotation) ?
                  getExisingSelection(firstRowElement, firstRowData) :
                  createNewSelection(firstRowIdx, firstRowElement);

            currentSelection = selection;
            self.fireEvent('select', selection);
          }
        },

        getSelection = function() {
          if (currentSelection) {
            // Client bounds may have changed in the meantime due to scrolling
            currentSelection.bounds = currentSelection.cell.getBoundingClientRect();
            return currentSelection;
          }
        },

        setSelection = function(selection) {

          // TODO implement (needed for #{id} direct links)

          console.log('set selection');
        },

        clearSelection = function() {
          currentSelection = false;
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
