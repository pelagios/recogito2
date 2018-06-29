import React, { Component } from 'react';
import axios from 'axios';

export default class UploadButton extends Component {

  submit(e) {
    const input = e.target;
    const data = new FormData();
    data.append('file', input.files[0]);

    axios.post('/admin/gazetteers', data)
      .then(response => {
        // Discard (for now)
      });
  }

  render() {
    return (
      <form className="upload">
        <input
          id="dumpfile"
          type="file"
          style={{display:'none'}}
          onChange={this.submit}/>

        <label className="btn" for="dumpfile">
          <span class="icon">&#xf055;</span> Upload Dump
        </label>
      </form>
    );
  }

}
