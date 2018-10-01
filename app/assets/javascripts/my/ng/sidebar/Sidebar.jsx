import React, { Component } from 'react';

import Account   from './sections/Account.jsx';
import CreateNew from './sections/CreateNew.jsx';
import ViewList  from './sections/Views.jsx';
import Storage   from './sections/Storage.jsx';

export default class Sidebar extends Component {

  render() {
    return (
      <div className="sidebar">
        <Account
          username="rainer"
          memberSince="2018-08-24T09:27:51+00:00" />

        <CreateNew />

        <ViewList
          currentView={this.props.currentView}
          onChangeView={this.props.onChangeView} />

        <Storage quotaMb={200} usedMb={15.3}/>
      </div>
    )
  }

}
