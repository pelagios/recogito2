import React, { Component } from 'react';

import Change from './Change.jsx';

export default class ChangesOverview extends Component {

  render() {
    return(
      <div className="changes-stack">
        <ul>
          <Change
            type="PLACE"
            value="pleiades:530906" />

          <Change
            type="TAG"
            value="Home" />
        </ul>
      </div>
    )
  }

}
