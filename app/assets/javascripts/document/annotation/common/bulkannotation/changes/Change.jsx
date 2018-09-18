import React, { Component } from 'react';

export default class Change extends Component {

  render() {
    return(
      <div className={`change ${this.props.action} ${this.props.type}`}>
        {this.props.value}
      </div>
    )
  }

}
