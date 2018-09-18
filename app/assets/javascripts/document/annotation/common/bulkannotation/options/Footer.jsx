import React, { Component } from 'react';

export default class Footer extends Component {

  render() {
    return(
      <div className="footer">
        <button
          className="btn outline"
          onClick={this.props.onCancel}>Cancel</button>

        <button className="btn">Apply Changes</button>
      </div>
    )
  }

}
