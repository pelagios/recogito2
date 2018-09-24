import React, { Component } from 'react';

import SelectableOption from '../SelectableOption.jsx';

export default class ApplyIf extends Component {

  constructor(props) {
    super(props);
    this.state = {
      checked: this.props.status != null,
      status: this.props.status || 'UNVERIFIED'
    };
  }

  fireStatusEvent() {
    const status = (this.state.checked) ? this.state.status : null;
    this.props.onChange({ applyIfStatus: status });
  }

  onToggleApplyIfStatus() {
    this.setState(previous => (
      { checked: !previous.checked }
    ), () => {
      this.fireStatusEvent()
    });
  }

  onChangeStatus(e) {
    const value = e.target.value;
    this.setState({ status: value }, () => {
      // Only forward events if checkbox is checked
      if (this.state.checked) this.fireStatusEvent();
    });
  }

  render() {
    return(
      <div className="section apply-if">
        {(this.props.mode == 'REAPPLY') ? (
          <h2>Apply this annotation if:</h2>
        ):(
          <h2>Delete annotations if:</h2>
        )}
        <ul>
          <SelectableOption
            id="if-phrase-matches"
            checked={true}
            readOnly={true}
            disabled={true}> phrase matches <em><u>{this.props.quote}</u></em>
          </SelectableOption>

          {this.mode == 'REAPPLY' &&
            <ul>
              <SelectableOption
                id="full-word"
                group="match-type"
                checked={this.props.matchType == 'FULL_WORD'}
                onChange={this.props.onChange.bind(this, {applyIfMatchType: 'FULL_WORD'})}
                type="radio"> require full word match (surrounded by whitespace, comma, etc.)
              </SelectableOption>

              <SelectableOption
                id="any-match"
                group="match-type"
                checked={this.props.matchType == 'ANY_MATCH'}
                onChange={this.props.onChange.bind(this, {applyIfMatchType: 'ANY_MATCH'})}
                type="radio"> allow any string match, including inside words
              </SelectableOption>
            </ul>
          }

          <SelectableOption
            id="if-status-is"
            checked={this.state.checked}
            onChange={this.onToggleApplyIfStatus.bind(this)} >

            annotation status is currently{' '}
            <select
              value={this.state.status}
              onChange={this.onChangeStatus.bind(this)}>
              <option value="UNVERIFIED">Not Verified</option>
              <option value="VERIFIED">Verified</option>
              <option value="NOT_IDENTIFIABLE">Flagged</option>
            </select>
          </SelectableOption>
        </ul>
      </div>
    )
  }
}
