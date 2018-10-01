import React, { Component } from 'react';

export default class AdvancedSearch extends Component {

  constructor(props) {
    super(props);

    this.onMousedown = this.onMousedown.bind(this);
    this.onKeydown = this.onKeydown.bind(this);
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.onMousedown, false);
    document.addEventListener('keydown', this.onKeydown, false);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.onMousedown, false);
    document.removeEventListener('keydown', this.onKeydown, false);
  }

  onMousedown(evt) {
    const isClickOutside = !this._node.contains(evt.target);
    if (isClickOutside) this.props.onClose();
  }

  onKeydown(evt) {
    if (evt.which == 27) this.props.onClose();
  }
  
  render() {
    return (
      <div
        ref={n => this._node = n}
        className="advanced-search">
        <button
          className="close nostyle"
          onClick={this.props.onClose}>&#xe897;</button>

        <form>
          <fieldset>
            <p>
              <label>Search in</label>
              <select>
                <option>All of Recogito</option>
                <option>My documents</option>
                <option>Shared with me</option>
              </select>
              <span className="hint" />
            </p>

            <p>
              <label>Document type</label>
              <select>
                <option>Any</option>
                <option>Text</option>
                <option>Image</option>
                <option>Table</option>
              </select>
              <span className="hint" />
            </p>

            <p>
              <label>Owner</label>
              <select>
                <option>Anyone</option>
                <option>me</option>
                <option>specific user</option>
              </select>
              <span className="hint" />
            </p>

            <p>
              <label>Metadata contains</label>
              <input type="text" />
              <span className="hint" />
            </p>

            <p>
              <label>Last modified</label>
              <select>
                <option>Any time</option>
                <option>this week</option>
              </select>
              <span className="hint" />
            </p>
          </fieldset>
          <fieldset className="buttons">
            <button className="nostyle clear">Clear</button>
            <button type="submit" className="btn">Search</button>
          </fieldset>
        </form>
      </div>
    )
  }

}
