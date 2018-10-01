import React, { Component } from 'react';

import MenuPopup from '../../common/MenuPopup.jsx';

export default class Account extends Component {

  constructor(props) {
    super(props);
    this.state = { menuVisible: false };
  }

  // https://medium.com/@pppped/compute-an-arbitrary-color-for-user-avatar-starting-from-his-username-with-javascript-cd0675943b66
  stringToHslColor(str) {
    let hash = 0;
    for (let i=0; i<str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return hash % 360;
  }

  onSelectMenuOption(option) {
    console.log(option);
  }

  showMenu() {
    this.setState({ menuVisible: true });
  }

  closeMenu() {
    this.setState({ menuVisible: false });
  }

  render() {
    return (
      <div
        className="sidebar-section account"
        onClick={this.showMenu.bind(this)} >

        <div className="avatar" style={{
          backgroundColor: `hsl(${this.stringToHslColor(this.props.username)}, 35%, 65%)`}}>
          <div className="inner">{this.props.username.charAt(0).toUpperCase()}</div>
        </div>

        <h1>{this.props.username}</h1>

        <p className="member-since">
          Joined on {new Intl.DateTimeFormat('en-GB', {
            year : 'numeric',
            month: 'short',
            day  : '2-digit'
          }).format(new Date(this.props.memberSince))}
        </p>

        {this.state.menuVisible &&
          <MenuPopup
            className="account-menu"
            menu={[
              { group: 'settings', options: [
                { icon: '\uf0ad', label: 'Account settings', value: 'SETTINGS' }
              ]},
              { group: 'general', options: [
                { icon: '\uf128', label: 'Help', value: 'HELP' },
                { icon: '\uf011', label: 'Sign out', value: 'SIGNOUT' }
              ]}
            ]}
            onSelect={this.onSelectMenuOption.bind(this)}
            onCancel={this.closeMenu.bind(this)} />
        }
      </div>
    )
  }

}
