import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class MatchSummary extends Component {

  render() {
    return (
      <div className="section match-stats">
        {(this.props.mode == 'REAPPLY') ? (
          'Your change will affect '
        ) : (
          'You are about to delete '
        )}
        <u className="total">{this.props.total}
        {(this.props.mode == 'REAPPLY') ? (' matches') : (' annotations')}
        </u>
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
