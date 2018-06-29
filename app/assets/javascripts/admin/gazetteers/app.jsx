import React, { Component } from 'react';
import { render } from 'react-dom';

import AuthorityList from './components/AuthorityList.jsx';
import AuthorityDetails from './components/AuthorityDetails.jsx';
import UploadButton from './components/UploadButton.jsx';

export default class App extends Component {

  onUploadDump() {
    console.log('upload');
  }

  render() {
    return (
      <div className="pane gazetteers">
        <div className="left">
          <AuthorityList />
          <UploadButton onSubmit={this.onUploadDump.bind(this)}/>
        </div>

        <div className="right">
          <AuthorityDetails />
        </div>
      </div>
    );
  }

}

render(<App />, document.getElementById('app'));
