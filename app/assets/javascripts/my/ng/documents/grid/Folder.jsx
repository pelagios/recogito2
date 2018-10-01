import React, { Component } from 'react';

export default class Folder extends Component {

  render() {
    return (
      <div className="cell">
        <div className="item-wrapper">
          <a href="#" className="folder">
            <div className="label">
              {this.props.name}
            </div>
          </a>
        </div>
      </div>
    )
  }

}
