import React, { Component } from 'react';
import { render } from 'react-dom';
import Draggable from 'react-draggable';

import { AnnotationList } from './common/AnnotationList.js';
import { AnnotationUtils } from './common/AnnotationUtils.js';
import Overview from './overview/Overview.jsx';
import OptionsPane from './options/OptionsPane.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);

    this.state = { open: false };

    const that = this;
    that.domNode = document.getElementById('bulk-annotation');
    that.domNode.addEventListener('open', function(evt) {
      const args = evt.args;
      that.setState({
        open: true,
        mode: args.mode,
        unannotatedMatches: args.unannotatedMatches,
        annotations: new AnnotationList(args.annotations),
        quote: AnnotationUtils.getQuote(args.original),
        original: args.original,
        uriParser: args.uriParser
      });
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
                    <Overview
                      mode={this.state.mode}
                      quote={this.state.quote}
                      original={this.state.original}
                      uriParser={this.state.uriParser /* legacy hack... */} />

                    <OptionsPane
                      mode={this.state.mode}
                      quote={this.state.quote}
                      original={this.state.original}
                      unannotatedMatches={this.state.unannotatedMatches}
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
