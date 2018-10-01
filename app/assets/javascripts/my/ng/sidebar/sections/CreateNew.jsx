import React, { Component } from 'react';

import MenuPopup from '../../common/MenuPopup.jsx';

export default class CreateNew extends Component {

  constructor(props) {
    super(props);
    this.state = { menuVisible: false };
  }

  onShowOptions() {
    this.setState({ menuVisible: true });
  }

  onSelectOption(option) {
    // TODO implement
    console.log(`selected ${option}`)
  }

  onCancel() {
    this.setState({ menuVisible: false });
  }

  render() {
    return (
      <div className="sidebar-section create-new">
        <button
          className="btn create-new"
          onClick={this.onShowOptions.bind(this)}>
          <span className="icon">&#xf067;</span>
          <span className="label">New</span>
        </button>
        {this.state.menuVisible &&
          <MenuPopup
            className="create-new"
            menu={[
              { group: 'local', options: [
                { icon: '\uf07b', label: 'Folder', value: 'FOLDER' },
                { icon: '\uf15b', label: 'File upload', value: 'FILE' }
              ]},

              { group: 'remote', options: [
                { icon: '\uf0c1', label: 'From IIIF manifest', value: 'IIIF' },
                { icon: '\uf121', label: 'From CTS service', value: 'CTS', disabled: true }
              ]}
            ]}
            onSelect={this.onSelectOption.bind(this)}
            onCancel={this.onCancel.bind(this)} />
        }
      </div>
    )
  }

}
