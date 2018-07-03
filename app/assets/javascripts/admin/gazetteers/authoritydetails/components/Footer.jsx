import React, { Component } from 'react';

export default class Footer extends Component {

  render() {
    return (
      <div className="footer">
        <button
          className="btn red"
          onClick={this.props.onDelete}>
          <span class="icon">&#xf1f8;</span> Delete
        </button>

        <button
          className="btn"
          onClick={this.props.onSubmit}>Save</button>
      </div>
    );
  }

}
