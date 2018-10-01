import React, { Component } from 'react';

export default class ColumnHeading extends Component {

  render() {
    return (
      <span
        className={`label ${this.props.className}`}
        onClick={this.props.onClick}>
        <span className="inner">
          {this.props.children}
          {this.props.sortColumn &&
            <span className="sort icon">
              <span className="inner">
                {(this.props.sortAsc) ? '\ue688' : '\ue682' }
              </span>
              </span>
          }
        </span>
      </span>
    )
  }

}
