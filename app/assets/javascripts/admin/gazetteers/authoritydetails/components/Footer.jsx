import React, { Component } from 'react';

export default class Footer extends Component {

  onDelete(evt) {
    const confirmed = window.confirm('Are you sure you want to delete this gazetteer?');
    if (confirmed) this.props.onDelete(evt);
  }

  render() {
    return (
      <div className="footer">
        <button
          className="btn red"
          onClick={this.onDelete.bind(this)}>
          <span class="icon">&#xf1f8;</span> Delete
        </button>

        <button
          className="btn"
          onClick={this.props.onSubmit}>Save</button>
      </div>
    );
  }

}
