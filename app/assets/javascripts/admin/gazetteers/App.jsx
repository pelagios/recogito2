import React, { Component } from 'react';
import { render } from 'react-dom';

import AuthorityList from './authoritylist/AuthorityList.jsx';
import AuthorityDetails from './authoritydetails/AuthorityDetails.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);
    this.state = { details : null };
  }

  componentDidMount(){
    const onKeydown = e => {
      if (e.which === 27) this.setState({ details: null });
    }

    document.addEventListener('keydown', onKeydown.bind(this), false);
  }

  addNew() {
    this.setState({ details: {} });
  }

  onSelect(authority) {
    this.setState({ details: authority });
  }

  onSaved() {
    // That's somewhat hacked, but not sure it's worth lifting state up?
    this._list.refresh();
  }

  render() {
    return (
      <div className="pane two">
        <div className="left">
          <AuthorityList
            ref={c => this._list = c}
            onSelect={this.onSelect.bind(this)}
            onAddNew={this.addNew.bind(this)}/>
        </div>

        {this.state.details &&
          <div className="right">
            <AuthorityDetails
              value={this.state.details}
              onSaved={this.onSaved.bind(this)}/>
          </div>
        }
      </div>
    );
  }

}

render(<App />, document.getElementById('app'));
