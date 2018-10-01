import React, { Component } from 'react';

export default class Readme extends Component {

  render() {
    return (
      <div className="readme">
        <div className="inner">
          {this.props.children}
        </div>
      </div>
    )
  }

}
