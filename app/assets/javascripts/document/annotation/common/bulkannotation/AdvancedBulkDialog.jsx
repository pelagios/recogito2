import React, { Component } from 'react';
import Draggable from 'react-draggable';
import axios from 'axios';

import { AnnotationList } from './common/annotationList.js';
import ChangesStack from './changesStack/ChangesStack.jsx';
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

  componentDidMount() {
    axios.get('test/resources/annotations.json')
      .then(result => {
        this.setState({ annotations: new AnnotationList(result.data) });
      });
  }

  onOk() {
    console.log('OK');
  }

  render() {
    return(
      <div className="clicktrap">
        <div className="modal-wrapper">
          <Draggable handle=".modal-header">
            <div className="modal advanced-bulk-options">
              <div className="advanced-bulk-options-header modal-header">
                <h1 className="title">Bulk Annotation</h1>
                <button className="cancel nostyle" onClick={this.props.onCancel}>&#xe897;</button>
              </div>

              <div className="advanced-bulk-options-body modal-body">
                <ChangesStack />

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
