import React, { Component } from 'react';

export default class Meter extends Component {

  render() {
    return (
      <div
        className="meter"
        style={{
          position: 'relative',
          width: this.props.width || '100%',
          height: this.props.height || '3px',
          backgroundColor: this.props.backgroundColor || '#e3e9eb'
        }}>

        <div
          className="bar"
          style={{
            width: `${100 * this.props.value}%`,
            height: '100%',
            backgroundColor: this.props.backgroundColor || '#4483c4'
          }} />
      </div>
    )
  }

}
