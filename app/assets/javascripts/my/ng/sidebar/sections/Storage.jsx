import React, { Component } from 'react';

import Meter from '../../common/Meter.jsx';

export default class Storage extends Component {

  render() {
    return (
      <div className="sidebar-section storage">
        <span className="icon">&#xf1c0;</span> Storage
        <div className="used-diskspace">
          <Meter
            value={this.props.usedMb / this.props.quotaMb}
            width="192px" />

          <div className="label">
            {this.props.usedMb} of {this.props.quotaMb} MB used
          </div>
        </div>
      </div>
    )
  }

}
