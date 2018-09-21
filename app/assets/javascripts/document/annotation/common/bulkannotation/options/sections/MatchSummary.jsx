import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class MatchSummary extends Component {

  getReapplyLabel(total) {
    if (this.props.annotated == 0 && this.props.unannotated > 0)
      return (
        <u className="total">{total} unannotated matches</u>
      );
    else if (this.props.annotated > 0 && this.props.unannotated == 0)
      return (
        <u className="total">{total} annotated matches</u>
      );
    else
      return (
        <u className="total">{total} matches</u>
      );
  }

  render() {
    return (
      <div className="section match-stats">
        {(this.props.mode == 'REAPPLY') ? (
          'Your change will affect '
        ) : (
          'You are about to delete '
        )}

        {(this.props.mode == 'REAPPLY') ?
          this.getReapplyLabel(this.props.total)
        : (
          <u className="total">{this.props.total} annotations</u>
        )}
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
