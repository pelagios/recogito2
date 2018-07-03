import React, { Component } from 'react';
import axios from 'axios';

import ColorField from './components/ColorField.jsx';
import FileField from './components/FileField.jsx';
import StringField from './components/StringField.jsx';

export default class AuthorityDetails extends Component {

  constructor(props) {
    super(props);
    this.state = {}; // TODO initial values?
  }

  closeMessage() {
    this.setState({
      success: null,
      error: null
    });
  }

  onAttachFile(evt) {
    const file = evt.target.files[0];
    this.file = file;
    this.setState({ filename: file.name });
  }

  /** Identifier and shortname are required properties **/
  validate() {
    const hasIdentifier = this.state.identifier;
    const hasShortname = this.state.shortname;

    if (!hasIdentifier && !hasShortname)
      this.setState({ error: 'Identifier and shortname are required properties' })
    else if (!hasIdentifier)
      this.setState({ error: 'Identifier is required' })
    else if (!hasShortname)
      this.setState({ error: 'Shortname is required' })
    else
      this.setState({ error: null });

    return hasIdentifier && hasShortname;
  }

  onChange(evt) {
    const input = evt.target;
    const state = {};
    state[input.name] = input.value;
    this.setState(state);
  }

  onSubmit() {
    const isValid = this.validate();

    if (isValid) {
      const data = new FormData();

      // Fields we need to pull from the state
      const stateFields = [
        'identifier',
        'shortname',
        'fullname',
        'shortcode',
        'color',
        'urlpatterns'
      ];

      stateFields.forEach(key => {
        const val = this.state[key]
        if (val) data.append(key, val);
      });

      // Append file, if any
      if (this.file) data.append('file', this.file);

      axios.post('/admin/gazetteers', data)
        .then(response => {
          this.setState({ success: response.data });
        })
        .catch(error => {
          this.setState({
            error: `Something went wrong: ${error.response.data}`
          });
        })
    }
  }

  render() {
    return (
      <div className="authority-details">
        {this.state.error &&
          <div className="error flash-message">
            <span
              className="icon"
              onClick={this.closeMessage.bind(this)}>&#xf00d;
            </span> {this.state.error}
          </div>
        }

        {this.state.success &&
          <div className="success flash-message">
            <span
              className="icon"
              onClick={this.closeMessage.bind(this)}>&#xf00c;
            </span> {this.state.success}
          </div>
        }

        <form className="crud">
          <StringField
            name="identifier"
            label="Identifier"
            onChange={this.onChange.bind(this)} />

          <StringField
            name="shortname"
            label="Short name"
            onChange={this.onChange.bind(this)} />

          <StringField
            name="fullname"
            label="Full name"
            onChange={this.onChange.bind(this)} />

          <StringField
            name="shortcode"
            label="Shortcode"
            onChange={this.onChange.bind(this)} />

          <StringField
            name="urlpatterns"
            label="URL Patterns"
            onChange={this.onChange.bind(this)} />

          <ColorField />

          <FileField
            name="file"
            label="Upload File"
            value={this.state.filename}
            buttonClass="btn add-file"
            onChange={this.onAttachFile.bind(this)}/>

          <dl>
            <dt/>
            <dd>
              <button
                type="button"
                className="btn"
                onClick={this.onSubmit.bind(this)}>Save</button>
            </dd>
          </dl>
        </form>
      </div>
    );
  }

}
