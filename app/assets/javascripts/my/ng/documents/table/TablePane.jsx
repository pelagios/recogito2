import React, { Component } from 'react';
import TimeAgo from 'react-timeago'
import { AutoSizer, List } from 'react-virtualized';

import HeaderIcon from '../../header/HeaderIcon.jsx';
import Readme from '../Readme.jsx';
import PreferencesModal from  './preferences/PreferencesModal.jsx';
import ColumnHeading from './ColumnHeading.jsx';
import FolderRow from './FolderRow.jsx';
import DocumentRow from './DocumentRow.jsx';

export default class TablePane extends Component {

  constructor(props) {
    super(props);
    this.state = {
      sortColumn: null,
      sortAsc: true,
      prefsOpen: false
    }
  }

  rowRenderer() {
    const allItems = this.props.folders.concat(this.props.documents);

    return ((args) => {
      const item = allItems[args.index];
      const isFolder = item.name;

      if (isFolder)
        return (
          <FolderRow key={args.key} style={args.style} name={item.name} />
        )
      else
        return (
          <DocumentRow
            key={args.key}
            style={args.style}
            filetypes={item.filetypes}
            id={item.id}
            author={item.author}
            title={item.title}
            language={item.language}
            date={item.date}
            uploaded={item.uploaded_at}
            lastEdit={item.lastedit} />
        )
    })
  }

  showPreferences(visible) {
    this.setState({ prefsOpen: visible });
  }

  sortBy(field) {
    this.setState(prev => {
      if (prev.sortColumn == field)
        return { sortAsc: !prev.sortAsc };
      else
        return {
          sortColumn: field,
          sortAsc: true
        };
    });
  }

  render() {
    const readme = React.Children.toArray(this.props.children)
      .filter(c => c.type == Readme)
      .shift(); // First readme or undefined

    // Shorthand
    const createHeading = (field, label) =>
      <ColumnHeading
        className={field}
        onClick={this.sortBy.bind(this, field)}
        sortColumn={this.state.sortColumn == field}
        sortAsc={this.state.sortAsc}>{label}
      </ColumnHeading>

    return (
      <React.Fragment>
        <div className="documents-table-header">
          <div className="column-labels">
            { createHeading('document',  'Document')  }
            { createHeading('language',  'Language')  }
            { createHeading('date',      'Date')      }
            { createHeading('uploaded',  'Uploaded')  }
            { createHeading('last-edit', 'Last edit') }
          </div>

          <HeaderIcon
            icon="&#xf141;"
            className="column-options-btn"
            onClick={this.showPreferences.bind(this, true)} />
        </div>

        {readme}

        <div className="documents-pane table-pane">
          <AutoSizer>
            {({ height, width }) => (
              <List
                className="virtualized-list"
                width={width}
                height={height}
                rowCount={this.props.folders.length + this.props.documents.length}
                rowHeight={47}
                rowRenderer={this.rowRenderer()} />
            )}
          </AutoSizer>
        </div>

        {this.state.prefsOpen &&
          <PreferencesModal
            onClose={this.showPreferences.bind(this, false)} />
        }
      </React.Fragment>
    )
  }

}
