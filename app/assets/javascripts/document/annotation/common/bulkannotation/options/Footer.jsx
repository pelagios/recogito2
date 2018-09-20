import React, { Component } from 'react';

export default class Footer extends Component {

  render() {
    return(
      <div className="footer">
        <button
          className="btn outline"
          onClick={this.props.onCancel}>Cancel</button>

        {(this.props.mode == 'REAPPLY') ? (
          <button
            className="btn"
            onClick={this.props.onOk}>Apply Changes</button>
        ):(
          <button
            className="btn"
            onClick={this.props.onOk}>Delete</button>
        )}
      </div>
    )
  }

}
