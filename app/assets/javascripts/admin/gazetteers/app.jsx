import React, { Component } from 'react';
import { render } from 'react-dom';

import AuthorityList from './authoritylist/AuthorityList.jsx';
import AuthorityDetails from './authoritydetails/AuthorityDetails.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      details : null
    };
  }

  componentDidMount(){
    const onKeydown = e => {
      if (e.which === 27) this.setState({ details: null });
    }

    document.addEventListener('keydown', onKeydown.bind(this), false);
  }

  addNew() {
    this.setState({
      details: {} // Set empty details
    });
  }

  onSelect(authority) {
    this.setState({details: authority});
  }

  render() {
    return (
      <div className="pane two">
        <div className="left">
          <AuthorityList onSelect={this.onSelect.bind(this)} />

          <button className="btn" onClick={this.addNew.bind(this)}>
            <span class="icon">&#xf055;</span> Add New
          </button>
        </div>

        {this.state.details &&
          <div className="right">
            <AuthorityDetails value={this.state.details} />
          </div>
        }
      </div>
    );
  }

}

render(<App />, document.getElementById('app'));
