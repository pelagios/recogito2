import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class MatchSummary extends Component {

  render() {
    return (
      <div className="section match-stats">
        Your change will affect{' '}
        <u className="total">{this.props.total} matches</u>
        {(this.props.annotated > 0 && this.props.unannotated > 0) &&
          <React.Fragment>
            {'\u2014 '}
            <span className="annotated">{this.props.annotated} annotated</span>{', '}
            <span className="unannotated">{this.props.unannotated} unannotated</span>
          </React.Fragment>
        }
      </div>
    )
  }

}
