import React, { Component } from 'react';
import { render } from 'react-dom';

import AdvancedBulkDialog from './AdvancedBulkDialog.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);

    this.state = { open : false };

    // Hack for testing only
    window.config = { isAdmin : false };

    const that = this;
    that.domNode = document.getElementById('app');
    that.domNode.addEventListener('react-open', function(evt) {
      that.setState({ open : true });
    });
  }

  onCancel() {
    this.setState({ open: false });
    this.fireEvent({ action: 'CANCEL' });
  }

  fireEvent(value) {
    var evt = new Event('react-response');
    evt.value = value;
    this.domNode.dispatchEvent(evt);
  }

  render() {
    return(
      <React.Fragment>
        {this.state.open &&
          <AdvancedBulkDialog
            onCancel={this.onCancel.bind(this)} />
        }
      </React.Fragment>
    )
  }

}

render(<App />, document.getElementById('app'));
