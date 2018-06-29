import React, { Component } from 'react';
import { render } from 'react-dom';

import UploadForm from './components/UploadForm.jsx';

export default class App extends Component {

  onUploadDump() {
    console.log('upload');
  }

  render() {
    return (
      <UploadForm onSubmit={this.onUploadDump.bind(this)}/>
    );
  }

}

render(<App />, document.getElementById('app'));
