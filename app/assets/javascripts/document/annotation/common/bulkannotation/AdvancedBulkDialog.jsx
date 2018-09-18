import React, { Component } from 'react';
import Draggable from 'react-draggable';

import { AnnotationList } from './common/annotationList.js';
import ChangeList from './changes/ChangeList.jsx';
import OptionsPane from './options/OptionsPane.jsx';

export default class AdvancedBulkOptions extends Component {

  constructor(props) {
    super(props);
    this.state = {
      mode: 'REAPPLY',
      quote: 'Olympus',
      unannotatedMatches: 5,
      annotations: new AnnotationList([])
    };
  }

  onOk(settings) {
    console.log(settings);
  }

  render() {
    return(
      <div className="clicktrap">
        <div className="bulkannotation-wrapper">
          <Draggable handle=".bulkannotation-header">
            <div className="bulkannotation">
              <div className="bulkannotation-header">
                <h1 className="title">Bulk Annotation</h1>
                <button className="cancel nostyle" onClick={this.props.onCancel}>&#xe897;</button>
              </div>

              <div className="bulkannotation-body">
                <ChangeList />

                <OptionsPane
                  quote={this.state.quote}
                  unannotatedMatches={5}
                  annotations={this.state.annotations}
                  onOk={this.onOk.bind(this)}
                  onCancel={this.props.onCancel} />
              </div>
            </div>
          </Draggable>
        </div>
      </div>
    )
  }

}
