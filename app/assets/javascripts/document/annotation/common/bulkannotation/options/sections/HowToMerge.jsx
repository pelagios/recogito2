import React, { Component } from 'react';

import NoAdminWarning from './NoAdminWarning.jsx';
import SelectableOption from '../SelectableOption.jsx';

export default class MergePolicy extends Component {

  render() {
    return(
      <div className="section how-to-merge">
        <h2>How to merge changes:</h2>
        <p className="description">
          You are applying changes to existing annotations. Select how you want
          them to be merged.
        </p>
        <ul>
          <SelectableOption
            type="radio"
            id="append"
            group="merge-policy"
            checked={this.props.value == 'APPEND' }
            onChange={this.props.onChange.bind(this, 'APPEND')} >
            <strong>Append</strong> changes, keep all existing
            tags, comments and entity matches
          </SelectableOption>

          <SelectableOption
            type="radio"
            id="replace"
            group="merge-policy"
            checked={this.props.value == 'REPLACE' }
            onChange={this.props.onChange.bind(this, 'REPLACE')} >
            <strong>Replace</strong> annotations, removing all
            previously existing tags, comments or entity matches
          </SelectableOption>

          <SelectableOption
            type="radio"
            id="mixed"
            group="merge-policy"
            checked={this.props.value == 'MIXED'}
            onChange={this.props.onChange.bind(this, 'MIXED')} >
            <strong>Mixed </strong> - append new tags and comments, but
            replace entity matches
          </SelectableOption>
        </ul>

        <NoAdminWarning visible={this.props.value == 'REPLACE'}/>
      </div>
    )
  }

}
