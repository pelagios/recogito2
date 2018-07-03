import React, { Component } from 'react';

export default class AuthorityRow extends Component {

  render() {
    return (
      <li onClick={evt => this.props.onClick(this.props.value)}>
        {this.props.value.conflicted &&
          <span className="icon conflicted-warning">&#xf071;</span>
        }
        <span className="label">{this.props.value.shortname}</span>
        <span className="count">{this.props.value.count}</span>
      </li>
    );
  }

}
