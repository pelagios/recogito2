import React, { Component } from 'react';

export default class FolderRow extends Component {

  render() {
    return (
      <div
        style={this.props.style}
        className="row folder">
        <a href="#" className="folder-icon">&#xf07b;</a>
        <a href="#" className="folder-name">{this.props.name}</a>
      </div>
    )
  }

}
