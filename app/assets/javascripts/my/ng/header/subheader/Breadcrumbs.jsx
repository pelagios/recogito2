import React, { Component } from 'react';

export default class Breadcrumbs extends Component {

  render() {
    return (
      <div className="breadcrumbs">
        <h2>{this.props.value}</h2>
      </div>
    )
  }

}
