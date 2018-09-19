import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class ApplyTo extends Component {

  toggle(name) {
    const next = {};
    next[name] = !this.props[name];
    this.props.onChange(next);
  }

  render() {
    // Disable selection whenever there's only one type of match available
    const disabled = this.props.annotatedMatches || this.props.unannotatedMatches;
    return (
      <div className="section apply-to">
        <h2>Apply this change to:</h2>
        <ul>
          <SelectableOption
            id="to-annotated"
            checked={this.props.applyToAnnotated}
            onChange={this.toggle.bind(this, 'applyToAnnotated')}
            disabled={disabled}>annotated matches</SelectableOption>

          <SelectableOption
            id="to-unannotated"
            checked={this.props.applyToUnannotated}
            onChange={this.toggle.bind(this, 'applyToUnannotated')}
            disabled={disabled}>unannotated matches</SelectableOption>
        </ul>
      </div>
    )
  }

}
