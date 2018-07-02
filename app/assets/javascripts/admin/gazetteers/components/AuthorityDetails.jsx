import React, { Component } from 'react';
import { SwatchesPicker } from 'react-color';
import axios from 'axios';

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
          <dl>
            <dt><label for="identifier">Identifier</label></dt>
            <dd>
              <input
                type="text"
                id="identifier"
                name="identifier"
                autocomplete="false"
                value={this.state.identifier}
                onChange={this.onChange.bind(this)} />
            </dd>
          </dl>

          <dl>
            <dt><label for="fullname">Short name</label></dt>
            <dd>
              <input
                type="text"
                id="shortname"
                name="shortname"
                autocomplete="false"
                value={this.state.screenname}
                onChange={this.onChange.bind(this)} />
            </dd>
          </dl>

          <dl>
            <dt><label for="fullname">Full name</label></dt>
            <dd>
              <input
                type="text"
                id="fullname"
                name="fullname"
                autocomplete="false"
                value={this.state.screenname}
                onChange={this.onChange.bind(this)} />
            </dd>
          </dl>

          <dl>
            <dt><label for="shortcode">Shortcode</label></dt>
            <dd>
              <input
                type="text"
                id="shortcode"
                name="shortcode"
                autocomplete="false"
                value={this.state.shortcode}
                onChange={this.onChange.bind(this)} />
            </dd>
          </dl>

          <dl>
            <dt><label for="urlpatterns">URL Patterns</label></dt>
            <dd>
              <input
                type="text"
                id="urlpatterns"
                name="urlpatterns"
                autocomplete="false"
                value={this.state.urlpatterns}
                onChange={this.onChange.bind(this)} />
            </dd>
          </dl>

          <dl>
            <dt><label for="color">Color</label></dt>
            <dd>
              {/* <SwatchesPicker /> */}
            </dd>
          </dl>

          <dl className="fileupload">
            <dt><label for="filename">Upload File</label></dt>
            <dd>
              <input
                type="text"
                id="filename"
                name="filename"
                disabled="true"
                value={this.state.filename} />

              <input
                type="file"
                id="file"
                name="file"
                style={{display:'none'}}
                onChange={this.onAttachFile.bind(this)}/>

              <label className="btn add-file" for="file">
                <span class="icon">&#xf055;</span>
              </label>
            </dd>
          </dl>

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
