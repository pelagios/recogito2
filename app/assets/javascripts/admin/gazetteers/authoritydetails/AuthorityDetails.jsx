import React, { Component } from 'react';
import axios from 'axios';

import ColorField from './components/ColorField.jsx';
import FileField from './components/FileField.jsx';
import Footer from './components/Footer.jsx';
import StringField from './components/StringField.jsx';

const EMPTY = {
  identifier  : '',
  shortname   : '',
  fullname    : '',
  homepage    : '',
  shortcode   : '',
  url_patterns: '',
  color       : ''
};

export default class AuthorityDetails extends Component {

  constructor(props) {
    super(props);

    this.state = {
      errorMessage  : null, // UI state: displayed error message
      successMessage: null, // UI state: displayed success message
      filename      : null, // Filename displayed in file input
      value         : props.value
    };
  }

  componentWillReceiveProps(nextProps) {
    const nextValue = Object.assign({}, EMPTY, nextProps.value);
    this.setState({ value: nextValue });
  }

  closeMessages() {
    this.setState({
      errorMessage  : null,
      successMessage: null
    });
  }

  onAttachFile(evt) {
    const file = evt.target.files[0];
    this.file = file;
    this.setState({ filename: file.name });
  }

  onChange(evt) {
    const input = evt.target;

    const diff = {};
    diff[input.name] = input.value;

    const updatedValue = Object.assign({}, this.state.value, diff);
    this.setState({ value: updatedValue });
  }

  onChangeColor(hex) {
    const updatedValue =
      Object.assign({}, this.state.value, { color: hex });
    this.setState({ value: updatedValue });
  }

  /** Identifier and shortname are required properties **/
  validate() {
    const hasIdentifier = this.state.value.identifier;
    const hasShortname = this.state.value.shortname;

    if (!hasIdentifier && !hasShortname)
      this.setState({ errorMessage: 'Identifier and shortname are required properties' })
    else if (!hasIdentifier)
      this.setState({ errorMessage: 'Identifier is required' })
    else if (!hasShortname)
      this.setState({ errorMessage: 'Shortname is required' })
    else
      this.setState({ errorMessage: null });

    return hasIdentifier && hasShortname;
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
        'homepage',
        'shortcode',
        'color',
        'url_patterns'
      ];

      stateFields.forEach(key => {
        const val = this.state.value[key]
        if (val) data.append(key, val);
      });

      // Append file, if any
      if (this.file) data.append('file', this.file);

      axios.post('/admin/gazetteers', data)
        .then(response => {
          this.setState({ successMessage: response.data });
          this.props.onSaved(this.state.value);
        })
        .catch(error => {
          this.setState({
            errorMessage: `Something went wrong: ${error.response}`
          });
        })
    }
  }

  onDelete() {
    console.log('delete');
  }

  render() {
    return (
      <div className="authority-details">
        {this.state.errorMessage &&
          <div className="error flash-message">
            <span
              className="icon"
              onClick={this.closeMessages.bind(this)}>&#xf00d;
            </span> {this.state.errorMessage}
          </div>
        }

        {this.state.successMessage &&
          <div className="success flash-message">
            <span
              className="icon"
              onClick={this.closeMessages.bind(this)}>&#xf00c;
            </span> {this.state.successMessage}
          </div>
        }

        <form className="crud">
          <StringField
            name="identifier"
            label="Identifier"
            value={this.state.value.identifier}
            onChange={this.onChange.bind(this)} />

          <StringField
            name="shortname"
            label="Short name"
            value={this.state.value.shortname}
            onChange={this.onChange.bind(this)} />

          <StringField
            name="fullname"
            label="Full name"
            value={this.state.value.fullname}
            onChange={this.onChange.bind(this)} />

          <StringField
            name="homepage"
            label="Homepage"
            value={this.state.value.homepage}
            onChange={this.onChange.bind(this)} />

          <StringField
            name="shortcode"
            label="Shortcode"
            value={this.state.value.shortcode}
            onChange={this.onChange.bind(this)} />

          <StringField
            name="url_patterns"
            label="URL Patterns"
            value={this.state.value.url_patterns}
            onChange={this.onChange.bind(this)} />

          <ColorField
            name="color"
            label="Color"
            value={this.state.value.color}
            onChange={this.onChangeColor.bind(this)} />

          <FileField
            name="file"
            label="Upload File"
            value={this.state.filename}
            buttonClass="btn add-file"
            onChange={this.onAttachFile.bind(this)}/>
        </form>

        <Footer
          onSubmit={this.onSubmit.bind(this)}
          onDelete={this.onDelete.bind(this)} />
      </div>
    );
  }

}
