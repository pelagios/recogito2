import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class ApplyTo extends Component {

  toggle(name) {
    const next = {};
    next[name] = !this.props[name];
    this.props.onChange(next);
  }

  render() {
    return (
      <div className="section apply-to">
        <h2>Apply this change to:</h2>
        <ul>
          <SelectableOption
            id="to-annotated"
            checked={this.props.applyToAnnotated}
            onChange={this.toggle.bind(this, 'applyToAnnotated')}
            disabled={this.props.disableAnnotated}>annotated matches</SelectableOption>

          <SelectableOption
            id="to-unannotated"
            checked={this.props.applyToUnannotated}
            onChange={this.toggle.bind(this, 'applyToUnannotated')}
            disabled={this.props.disableUnannotated}>unannotated matches</SelectableOption>
        </ul>
      </div>
    )
  }

}
