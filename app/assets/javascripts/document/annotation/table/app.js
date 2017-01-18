require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/alert',
  'common/api',
  'common/config',
  'document/annotation/common/editor/editorRead',
  'document/annotation/common/editor/editorWrite',
  'document/annotation/common/page/loadIndicator',
  'document/annotation/common/baseApp',
  'document/annotation/table/bulk/bulkEditorPlace',
  'document/annotation/table/page/toolbar',
  'document/annotation/table/selection/highlighter',
  'document/annotation/table/selection/selectionHandler'
], function(
  Alert,
  API,
  Config,
  ReadEditor,
  WriteEditor,
  LoadIndicator,
  BaseApp,
  PlaceBulkEditor,
  Toolbar,
  Highlighter,
  SelectionHandler) {

  var loadIndicator = new LoadIndicator();

  var App = function(results) {

    var self = this,

        contentNode = document.getElementById('table-container'),

        toolbar = new Toolbar(jQuery('.header-toolbar')),

        dataView = new Slick.Data.DataView(),

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
          { id: '__annotation', name: '', field: '__annotation', width: 145, formatter: Highlighter.CellFormatter, resizable: false },
          { id: 'idx', name: '#', field: 'idx', width: 65, sortable: true }
        ],

        columns = jQuery.map(results.meta.fields, function(f) {
          return { id: f, name: f, field: f, sortable: true };
        }),

        grid = new Slick.Grid('#table', dataView, frozenColumns.concat(columns), options),

        data = results.data.map(function(f, idx) {
          f.idx = idx;

          // SlickGrid needs an ID field, use index unless the CSV has one already
          // TODO in case there's an ID field in the CSV already, check if it is unique
          if (!f.id) f.id = idx;

          return f;
        }),

        highlighter = new Highlighter(grid),

        selector = new SelectionHandler(grid, highlighter),

        editor = (Config.writeAccess) ?
          new WriteEditor(contentNode, selector) :
          new ReadEditor(contentNode),

        onBulkAnnotation = function(type) {
          if (type === 'PLACE')
            new PlaceBulkEditor(results.meta, !highlighter.isEmpty());
        },

        onRowCountChanged = function(e, args) {
          grid.updateRowCount();
          grid.render();
        },

        onRowsChanged = function(e, args) {
          grid.invalidateRows(args.rows);
          grid.render();
        },

        onSort = function(e, args) {
          var comparator = function(a, b) {
                var fieldA = a[args.sortCol.field],
                    fieldB = b[args.sortCol.field];
                return (fieldA == fieldB ? 0 : (fieldA > fieldB ? 1 : -1));
              };

          dataView.sort(comparator, args.sortAsc);
        },

        reapplyAnnotation = function(annotationToReapply) {
          var multiSelection = selector.getMultiSelection(),

              cloneBodies = function(bodies) {
                return bodies.map(function(body) {
                  return jQuery.extend({}, body);
                });
              },

              updatedAnnotations = [],

              annotation;

          if (multiSelection.length > 1) {
            for (var i=1, len=multiSelection.length; i<len; i++) {
              annotation = multiSelection[i].annotation;

              // Copy bodies from, replacing original one
              annotation.bodies = cloneBodies(annotationToReapply.bodies);
              highlighter.refreshAnnotation(annotation);
              updatedAnnotations.push(annotation);
            }

            // TODO there might be a race condition for the indicator when
            // TODO the successful storage of the first annotation overwrites the indication
            // TODO of the bulk operation
            self.header.showStatusSaving();
            API.bulkUpsertAnnotations(updatedAnnotations)
               .done(function(annotations) {
                 annotations.forEach(highlighter.refreshAnnotation);
                 self.header.showStatusSaved();
               })
               .fail(function(error) {
                 self.header.showSaveError(error);
               });
          }
        },

        onCreateAnnotation = function(selection) {
          reapplyAnnotation(selection.annotation);
          self.onCreateAnnotation(selection);
        },

        onUpdateAnnotation = function(annotation) {
          reapplyAnnotation(annotation);
          self.onUpdateAnnotation(annotation);
        },

        /** Determines whether the table DIV should have overflow set to hidden or visible **/
        setClipping = function(clip) {
          var overflow = (clip) ? 'hidden' : 'visible';
          contentNode.style.overflow = overflow;
        },

        onScroll = function(e, args) {
          var selection = selector.getSelection(),
              viewport = grid.getViewport(),

              isSelectionVisible = function(selectedRows) {
                var isVisible = false;
                selectedRows.forEach(function(idx) {
                  if (idx >= viewport.top && idx < viewport.bottom)
                    isVisible = true;
                });
                return isVisible;
              };

          if (selection) {
            if (isSelectionVisible(grid.getSelectedRows()))
              setClipping(false);
            else
              setClipping(true);

            editor.setPosition(selection.bounds);
          }
        };

    toolbar.on('bulkAnnotation', onBulkAnnotation);

    grid.onSort.subscribe(onSort);
    grid.onScroll.subscribe(onScroll);

    dataView.onRowCountChanged.subscribe(onRowCountChanged);
    dataView.onRowsChanged.subscribe(onRowsChanged);
    dataView.setItems(data);

    BaseApp.apply(this, [ highlighter, selector ]);

    selector.on('select', editor.openSelection);

    editor.on('createAnnotation', onCreateAnnotation);
    editor.on('updateAnnotation', onUpdateAnnotation);
    editor.on('deleteAnnotation', this.onDeleteAnnotation.bind(this));

    jQuery(window).resize(function() { grid.resizeCanvas(); });

    API.listAnnotationsInPart(Config.documentId, Config.partSequenceNo)
       .done(this.onAnnotationsLoaded.bind(this)).then(loadIndicator.destroy)
       .fail(this.onAnnotationsLoadError.bind(this)).then(loadIndicator.destroy);
  };
  App.prototype = Object.create(BaseApp.prototype);

  /** On page load, fetch and parse the CSV and instantiate the app **/
  jQuery(document).ready(function() {
    var dataURL = jsRoutes.controllers.document.DocumentController
          .getDataTable(Config.documentId, Config.partSequenceNo).absoluteURL(),

        onLoadComplete = function(results) {
          new App(results);
        },

        onLoadError = function(error) {
          var title = 'Error',
              message = 'There was an error loading the CSV file.',
              alert = new Alert(Alert.ERROR, title, message);
        };

    loadIndicator.init(document.getElementById('table-container'));

    Papa.parse(dataURL, {
      download : true,
      header   : true, // TODO can we make this configurable through extra table meta?
      error    : onLoadError,
      complete : onLoadComplete
    });
  });

});
