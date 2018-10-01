import React, { Component } from 'react';

export default class Document extends Component {

  render() {
    const type = this.props.filetypes[0]; // TODO make more clever in the future
    return (
      <div className="cell">
        <div className={`item-wrapper${this.props.fileCount ? ' stacked' : ''}`}>
          <a href={`/document/${this.props.id}/part/1/edit`} className={`document ${type}`}>
            <div className="label">
              {this.props.title}
            </div>
          </a>
        </div>
      </div>
    )
  }

}
