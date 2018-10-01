import React, { Component } from 'react';

import Search from './search/Search.jsx';
import HeaderIcon from './HeaderIcon.jsx';
import Breadcrumbs from './subheader/Breadcrumbs.jsx';

export default class Header extends Component {

  render() {
    return (
      <div className="header">
        <div className="top-row">
          <Search/>
          <div className="header-icons">
            <HeaderIcon icon="&#xf059;" className="help" />
            <HeaderIcon icon="&#xf080;" className="stats" />
            <HeaderIcon icon="&#xf0f3;" className="notifications" />
          </div>
        </div>
        <div className="subheader">
          <Breadcrumbs value="My Documents"/>
          <div className="subheader-icons">
            <HeaderIcon
              className={`presentation-toggle ${this.props.presentation}`}
              onClick={this.props.onTogglePresentation} />
          </div>
        </div>
      </div>
    )
  }

}
