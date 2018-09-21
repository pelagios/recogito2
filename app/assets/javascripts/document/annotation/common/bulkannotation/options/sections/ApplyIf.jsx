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

  fireEvent() {
    const status = (this.state.checked) ? this.state.status : null;
    this.props.onChange(status);
  }

  onToggleApplyIfStatus() {
    this.setState(previous => (
      { checked: !previous.checked }
    ), () => {
      this.fireEvent()
    });
  }

  onChangeStatus(e) {
    const value = e.target.value;
    this.setState({ status: value }, () => {
      // Only forward events if checkbox is checked
      if (this.state.checked) this.fireEvent();
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
            id="when-phrase-matches"
            checked={true}
            readOnly={true}
            disabled={true}> phrase matches <em><u>{this.props.quote}</u></em>
          </SelectableOption>

          <SelectableOption
            id="when-status-is"
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
