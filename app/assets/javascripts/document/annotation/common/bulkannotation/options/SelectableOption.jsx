import React, { Component } from 'react';

export default class SelectableOption extends Component {

  // For future extension
  render() {
    return (
      <li>
        {(this.props.type == 'radio') ? (
          <input
            type="radio"
            id={this.props.id}
            name={this.props.group}
            checked={this.props.checked}
            onChange={this.props.onChange}
            readOnly={!this.props.onChange}/>
        ) : (
          <input
            type="checkbox"
            id={this.props.id}
            name={this.props.id}
            checked={this.props.checked}
            onChange={this.props.onChange}
            readOnly={!this.props.onChange} />
        )}
        <label htmlFor={this.props.id}>
          {this.props.children}
        </label>
      </li>
    )
  }

}
