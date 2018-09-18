import React, { Component } from 'react';
import { render } from 'react-dom';

import AdvancedBulkDialog from './AdvancedBulkDialog.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);

    this.state = { open : false };

    const that = this;
    that.domNode = document.getElementById('bulk-annotation');
    that.domNode.addEventListener('open', function(evt) {
      that.setState({ open : true });
    });
  }

  fireEvent(value) {
    var evt = new Event('response');
    evt.value = value;
    this.domNode.dispatchEvent(evt);
  }

  onCancel() {
    this.setState({ open: false });
    this.fireEvent({ action: 'CANCEL' });
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

render(<App />, document.getElementById('bulk-annotation'));
