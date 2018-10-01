import React, { Component } from 'react';

import Modal from '../../../common/Modal.jsx';
import ColumnOrder from './ColumnOrder.jsx';

const ROWS = [
  { name: 'document',     label: 'Document' },
  { name: 'language',     label: 'Language' },
  { name: 'date',         label: 'Date' },
  { name: 'uploaded',     label: 'Uploaded at' },
  { name: 'last_edit',    label: 'Last edit' },
  { name: 'by',           label: 'Last edit by' },
  { name: 'annotations',  label: 'No. of annotations' },
  { name: 'is_public',    label: 'Open to public'},
  { name: 'status_ratio', label: 'Verification ratio'},
  { name: 'activity',     label: 'Activity graph'},

  // Shared documents
  { name: 'owner',        label: 'Document owner' },
  { name: 'shared_by',    label: 'Shared by'},
  { name: 'access_level', label: 'Access level' }
];

export default class PreferencesModal extends Component {

  constructor(props) {
    super(props);

    // TODO replace with actual user setting
    this.state = {
      document: true,
      language: true,
      date: true,
      uploaded: true,
      last_edit: true,
      by: false,
      annotations: false,
      is_public: false,
      owner: false,
      shared_by: false,
      access_level: false
    };
  }

  /** Filters the row array by the visibility checkbox state **/
  getVisibleRows() {
    return ROWS.filter(row => this.state[row.name]);
  }

  /** Sets visibility for all rows to the given boolean value **/
  setAllRows(checked) {
    const diff = {};
    ROWS.forEach(r => { diff[r.name] = checked; });
    this.setState(diff);
  }

  /** Returns true if all rows are currently checked (i.e. visible) **/
  allRowsChecked() {
    const unchecked = Object.entries(this.state).find(e => {
      return e[1] == false;
    });
    return !unchecked;
  }

  /** Toggles the checkbox with the given name **/
  toggleOne(name) {
    this.setState(prev => {
      const next = {};
      next[name] = !prev[name];
      return next;
    });
  }

  /**
   * Clicking 'ALL' either checks all boxes or - if all are currently
   * checked, unchecks them.
   */
  onClickAll() {
    const allRowsChecked = this.allRowsChecked();
    if (allRowsChecked)
      this.setAllRows(false);
    else
      this.setAllRows(true);
  }

  /** Callback after the user has re-sorted the column order **/
  onSortEnd(items) {
    // TODO
    console.log(items);
  }

  render() {
    // Helper to create a single checkbox + label row
    const createRow = (name, label) =>
      <li key={name}>
        <input
          type="checkbox"
          id={name}
          name={name}
          checked={this.state[name] || false}
          onChange={this.toggleOne.bind(this, name)} />
        <label htmlFor={name}>{label}</label>
      </li>

    return (
      <Modal
        className="preferences"
        title="Column Preferences"
        onClose={this.props.onClose}>

        <div className="wrapper">
          <div className="selected-columns">
            <button
              className="all nostyle"
              onClick={this.onClickAll.bind(this)}>All</button>
            <ul>
              { ROWS.map(r => createRow(r.name, r.label)) }
            </ul>
          </div>

          <ColumnOrder
            items={this.getVisibleRows()}
            onSortEnd={this.onSortEnd.bind(this)} />
        </div>
      </Modal>
    )
  }

}
