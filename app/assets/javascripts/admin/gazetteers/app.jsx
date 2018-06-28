import React, { Component } from 'react';
import { render } from 'react-dom';

export default class App extends Component {

  render() {
    return (
      <div className='container'>I'm ALIVE!</div>
    );
  }
}

render(<App />, document.getElementById('app'));
