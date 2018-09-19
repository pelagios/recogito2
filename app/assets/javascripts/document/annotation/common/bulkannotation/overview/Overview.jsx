import React, { Component } from 'react';

import Body from './Body.jsx';
import Delete from './Delete.jsx';

export default class Overview extends Component {

  /** Filters the bodies to only those that are relevant for the overview **/
  getRelevantBodies(annotation) {
    return annotation.bodies.filter(b => {
      return b.type != 'QUOTE' && b.type != 'TRANSCRIPTION';
    });
  }

  render() {
    return(
      <div className="overview">
        {(this.props.mode == 'REAPPLY' && this.props.original) ?
          this.getRelevantBodies(this.props.original).map((body, index) =>
            <Body
              key={index}
              quote={this.props.quote}
              data={body} />
          ) : (
            <Delete />
          )
        }
      </div>
    )
  }

}
