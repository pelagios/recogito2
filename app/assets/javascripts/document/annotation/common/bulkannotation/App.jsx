import React, { Component } from 'react';
import { render } from 'react-dom';
import Draggable from 'react-draggable';

import { AnnotationList } from './common/annotationList.js';
import ChangeList from './changes/ChangeList.jsx';
import OptionsPane from './options/OptionsPane.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);

    this.state = {
      open: false,
      mode: 'REAPPLY',
      quote: 'Olympus',
      unannotatedMatches: 5,
      annotations: new AnnotationList([])
    };

    const that = this;
    that.domNode = document.getElementById('bulk-annotation');
    that.domNode.addEventListener('open', function(evt) {
      console.log(evt.args);
      that.setState({ open : true });
    });
  }

  fireEvent(type, value) {
    var evt = new Event(type);
    if (value) evt.value = value;
    this.domNode.dispatchEvent(evt);
  }

  onOk(settings) {
    this.fireEvent('ok', settings);
  }

  onCancel() {
    this.setState({ open: false });
    this.fireEvent('cancel');
  }

  render() {
    return(
      <React.Fragment>
        {this.state.open &&
          <div className="clicktrap">
            <div className="bulkannotation-wrapper">
              <Draggable handle=".bulkannotation-header">
                <div className="bulkannotation">
                  <div className="bulkannotation-header">
                    <h1 className="title">Bulk Annotation</h1>
                    <button className="cancel nostyle" onClick={this.onCancel.bind(this)}>&#xe897;</button>
                  </div>

                  <div className="bulkannotation-body">
                    <ChangeList />

                    <OptionsPane
                      quote={this.state.quote}
                      unannotatedMatches={5}
                      annotations={this.state.annotations}
                      onOk={this.onOk.bind(this)}
                      onCancel={this.onCancel.bind(this)} />
                  </div>
                </div>
              </Draggable>
            </div>
          </div>
        }
      </React.Fragment>
    )
  }

}

render(<App />, document.getElementById('bulk-annotation'));
