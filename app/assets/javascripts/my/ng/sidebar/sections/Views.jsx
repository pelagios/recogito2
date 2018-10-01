import React, { Component } from 'react';

class ViewListItem extends Component {

  render() {
    return (
      <li
        className={this.props.current ? 'current' : null}
        onClick={this.props.onClick}>
        <span className="icon">{this.props.icon}</span> {
          this.props.children
        } {this.props.count &&
          <span className="doc-count badge">{this.props.count}</span>
        }
      </li>
    )
  }

}

export default class ViewList extends Component {

  render() {
    return (
      <div className="sidebar-section views">
        <ul>
          <ViewListItem
            icon="&#xf2be;"
            count={9}
            current={this.props.currentView == 'MY_DOCUMENTS'}
            onClick={this.props.onChangeView.bind(this, 'MY_DOCUMENTS')}>
            My Documents
          </ViewListItem>

          <ViewListItem
            icon="&#xf064;"
            count={41}
            current={this.props.currentView == 'SHARED_WITH_ME'}
            onClick={this.props.onChangeView.bind(this, 'SHARED_WITH_ME')}>
            Shared with me
          </ViewListItem>

          <ViewListItem
            icon="&#xf006;"
            count={4}
            current={this.props.currentView == 'STARRED'}
            onClick={this.props.onChangeView.bind(this, 'STARRED')}>
            Starred
          </ViewListItem>

          <ViewListItem
            icon="&#xf017;"
            current={this.props.currentView == 'RECENT'}
            onClick={this.props.onChangeView.bind(this, 'RECENT')}>
            Recent
          </ViewListItem>
        </ul>
      </div>
    )
  }

}
