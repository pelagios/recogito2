import React, { Component } from 'react';
import { render } from 'react-dom';

import AuthorityList from './components/AuthorityList.jsx';
import AuthorityDetails from './components/AuthorityDetails.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      details : null
    };
  }

  addNew() {
    this.setState({
      details: {} // Set empty details      
    });
  }

  render() {
    return (
      <div className="pane gazetteers">
        <div className="left">
          <AuthorityList />
          <button className="btn" onClick={this.addNew.bind(this)}>
            <span class="icon">&#xf055;</span> Add New
          </button>
        </div>

        <div className="right">
          {this.state.details &&
            <AuthorityDetails detailsFor={this.state.details} />
          }
        </div>
      </div>
    );
  }

}

render(<App />, document.getElementById('app'));
