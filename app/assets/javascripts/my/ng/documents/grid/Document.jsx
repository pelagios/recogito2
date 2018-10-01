import React, { Component } from 'react';

export default class Document extends Component {

  render() {
    const type = this.props.filetypes[0]; // TODO make more clever in the future
    return (
      <div className="cell">
        <div className={`item-wrapper${this.props.fileCount ? ' stacked' : ''}`}>
          { this.props.fileCount && <div className="stack" /> }
          <a href="#" className={`document ${type}`}>
            <div className="label">
              {this.props.title}
            </div>
          </a>
        </div>
      </div>
    )
  }

}
