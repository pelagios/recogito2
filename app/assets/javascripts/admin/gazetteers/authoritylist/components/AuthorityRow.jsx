import React, { Component } from 'react';

export default class AuthorityRow extends Component {

  render() {
    return (
      <tr onClick={evt => this.props.onClick(this.props.value)}>
        {this.props.value.conflicted ?
          <td><span className="icon conflicted-warning">&#xf071;</span></td> :
          <td></td>
        }
        <td>{this.props.value.identifier}</td>
        <td>{this.props.value.shortname}</td>
        <td>{this.props.value.count}</td>
      </tr>
    );
  }

}
