import React, { Component } from 'react';

const fmt = new Intl.NumberFormat('en-US')

export default class AuthorityRow extends Component {

  render() {
    return (
      <tr onClick={evt => this.props.onClick(this.props.value)}>
        {this.props.value.conflicted ?
          <td className="conflicted-warning"><span className="icon">&#xf071;</span></td> :
          <td></td>
        }
        <td>{this.props.value.identifier}</td>
        <td>{this.props.value.shortname}</td>
        <td className="align-right">{fmt.format(this.props.value.count)}</td>
      </tr>
    );
  }

}
