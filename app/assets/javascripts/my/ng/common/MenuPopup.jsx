import React, { Component } from 'react';

export default class MenuPopup extends Component {

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
    if (isClickOutside) this.props.onCancel();
  }

  onKeydown(evt) {
    if (evt.which == 27) this.props.onCancel();
  }

  render() {
    return (
      <div
        ref={n => this._node = n}
        className={`modal-menu ${this.props.className}`}>
        <ul>
          {this.props.menu.map(group =>
            <li
              key={group.group}
              className={`group ${group.group}`}>

              <ul>
                {group.options.map(option =>
                  <li
                    key={option.value}
                    className={`option${option.disabled ? ' disabled' : '' }`}
                    onClick={option.disabled ? null : this.props.onSelect.bind(this, option.value)}>
                    <span className="icon">{option.icon}</span> {option.label}
                  </li>
                )}
              </ul>
            </li>
          )}
        </ul>
      </div>
    )
  }

}
