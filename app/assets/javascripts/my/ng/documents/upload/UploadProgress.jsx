import React, { Component } from 'react';

import Meter from '../../common/Meter.jsx';

export default class UploadProgress extends Component {

  render() {
    return (
      <div className="upload-progress">
        <div className="phase">
          {this.props.phase}
          <button className="close nostyle">&#xe897;</button>
        </div>
        <ul className="files">
          {this.props.files.map(f =>
            <li>
              {f}
              <span className="icon spinner spin"></span>
            </li>
          )}
        </ul>
        <div className="progress">
          <Meter value={this.props.progress} />
        </div>
      </div>
    )
  }

}
