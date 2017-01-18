define([
  'common/config',
  'document/annotation/common/selection/abstractSelectionHandler'
], function(Config, AbstractSelectionHandler) {

  var SelectionHandler = function(grid, highlighter) {

    var self = this,

        dataView = grid.getData(),

        leftViewPort = jQuery('.slick-viewport-left'),

        currentMultiSelection = false,

        /** Returns the first (in terms of row index) cell of the selection **/
        getFirstSelectedCell = function() {
          var selectedRow = grid.getActiveCellNode().closest('.slick-row.active'),
              isFrozenRow = selectedRow.parents('.slick-viewport-left').length > 0;

          // Popup should always hover over the first cell, which is part of the frozen row
          if (!isFrozenRow)
            selectedRow = leftViewPort.find('.slick-cell.frozen.selected').parent();

          return selectedRow.find('.l0')[0];
        },

        onSelectedRowsChanged = function(e, args) {
          var createNewSelection = function(row, cell, bounds) {
                var annotationStub = {
                      annotates: {
                        document_id: Config.documentId,
                        filepart_id: Config.partId,
                        content_type: Config.contentType
                      },
                      anchor: 'row:' + row.idx,
                      bodies: []
                    };

                return {
                  isNew: true,
                  annotation: annotationStub,
                  bounds: bounds,
                  cell: cell
                };
              },

              getExisingSelection = function(row, cell, bounds) {
                return {
                  isNew: false,
                  annotation: row.__annotation,
                  bounds: bounds,
                  cell: cell
                };
              };

          if (args.rows.length > 0) {
            var anchorCell = getFirstSelectedCell(),

                bounds = anchorCell.getBoundingClientRect(),

                rowIndices = args.rows.map(function(str) { return parseInt(str); }).sort();

            currentMultiSelection = rowIndices.map(function(idx) {
              var row = dataView.getItem(idx);
              return (row.__annotation) ?
                getExisingSelection(row, anchorCell, bounds) :
                createNewSelection(row, anchorCell, bounds);
            });

            self.fireEvent('select', currentMultiSelection[0]);
          }
        },

        getFirstSelected = function() {
          var firstSelected;

          if (currentMultiSelection) {
            firstSelected = currentMultiSelection[0];
            // Client bounds may have changed in the meantime due to scrolling
            firstSelected.cell = getFirstSelectedCell();
            firstSelected.bounds = firstSelected.cell.getBoundingClientRect();
            return firstSelected;
          }
        },

        getMultiSelection = function() {
          return currentMultiSelection;
        },

        setSelection = function(selection) {

          // TODO implement (needed for #{id} direct links)

          console.log('set selection');
        },

        clearSelection = function() {
          currentMultiSelection = false;
          grid.setSelectedRows([]);
        };

    grid.setSelectionModel(new Slick.RowSelectionModel());
    grid.onSelectedRowsChanged.subscribe(onSelectedRowsChanged);

    // This method is for general Recogito interop and expects one selection only
    this.getSelection = getFirstSelected;
    this.getMultiSelection = getMultiSelection;
    this.setSelection = setSelection;
    this.clearSelection = clearSelection;

    AbstractSelectionHandler.apply(this);
  };
  SelectionHandler.prototype = Object.create(AbstractSelectionHandler.prototype);

  return SelectionHandler;

});
