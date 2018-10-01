import React, { Component } from 'react';
import Dropzone from 'react-dropzone'

import { AutoSizer, List } from 'react-virtualized';

import Folder from './Folder.jsx';
import Document from './Document.jsx';
import Readme from '../Readme.jsx';

import DropzoneDecoration from '../DropzoneDecoration.jsx';

const ITEM_SIZE = 190;

/**
 * Using the following example:
 *
 * https://stackoverflow.com/questions/46368305/how-to-make-a-list-grid-with-a-responsive-number-of-columns-using-react-virtuali
 * http://plnkr.co/edit/zjCwNeRZ7XtmFp1PDBsc?p=preview
 */
export default class GridPane extends Component {

  constructor(props) {
    super(props);
    this.state = { drag: false };
  }

  rowRenderer(itemsPerRow, rowCount) {
    const allItems = this.props.folders.concat(this.props.documents);

    return ((args) => {
      const fromIndex = args.index * itemsPerRow;
      const toIndex = Math.min(fromIndex + itemsPerRow, allItems.length);
      const itemsInRow = toIndex - fromIndex;

      const renderedItems = new Array(itemsInRow).fill(undefined).map((_, rowIdx) => {
        const idx = rowIdx + fromIndex;
        const item = allItems[idx];

        const isFolder = item.name; // Dummy condition
        if (isFolder)
          return (
            <Folder key={idx} name={item.name} />
          )
        else
          return (
            <Document
              key={idx}
              title={item.title}
              filetypes={item.filetypes}
              fileCount={item.file_count} />
          )
      });

      if (itemsInRow < itemsPerRow) // Add dummies to preserve grid layout
        renderedItems.push(new Array(itemsPerRow - itemsInRow).fill(undefined).map((_, idx) =>
          <div className="cell dummy" key={`dummy-${idx}`} />
        ))

      return (
        <div
          key={args.key}
          style={args.style}
          className="row">
          {renderedItems}
        </div>
      )
    });
  }

  onDrag(active) {
    this.setState({ drag: active });
  }

  render() {
    const readme = React.Children.toArray(this.props.children)
      .filter(c => c.type == Readme)
      .shift(); // First readme or undefined

    return (
      <React.Fragment>
        {readme}
        <div className="documents-pane grid-pane">
          <Dropzone
            className="dropzone"
            disableClick
            style={{ height: '100%' }}
            onDragEnter={this.onDrag.bind(this, true)}
            onDragLeave={this.onDrag.bind(this, false)}
            onDrop={this.props.onDropFile}>

            <AutoSizer>
              {({ height, width }) => {
                const itemCount = this.props.folders.length + this.props.documents.length;
                const itemsPerRow = Math.floor(width / ITEM_SIZE);
                const rowCount = Math.ceil(itemCount / itemsPerRow);

                return (
                  <List
                    className="virtualized-grid"
                    width={width}
                    height={height}
                    rowCount={rowCount}
                    rowHeight={ITEM_SIZE}
                    rowRenderer={this.rowRenderer(itemsPerRow, rowCount)} />
                )
              }}
            </AutoSizer>
          </Dropzone>

          { this.state.drag && <DropzoneDecoration /> }
        </div>
      </React.Fragment>
    )
  }

}
