import React, { Component } from 'react';
import { SwatchesPicker } from 'react-color';

export default class ColorField extends Component {

  constructor(props) {
    super(props);
    this.state = { color: this.props.value };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({ color: nextProps.value });
  }

  toggleColorPicker() {
    this.setState(previous => ({
      isColorPickerOpen: !previous.isColorPickerOpen
    }));
  }

  /** Manual color input on the text field **/
  onType(evt) {
    const color = evt.target.value;
    // TODO validate color
    this.setState({ color: color });
    this.props.onChange(color);
  }

  /** Color input via the picker **/
  onPick(color) {
    this.props.onChange(color.hex);
    this.setState({ color: color.hex });
    this.setState({ isColorPickerOpen: false });
  }

  render() {
    return (
      <dl className="color">
        <dt><label for={this.props.name}>{this.props.label}</label></dt>
        <dd>
          <input
            type="text"
            id={this.props.name}
            name={this.props.name}
            value={this.state.color}
            onChange={this.onType.bind(this)} />

          <span
            className="color-sample"
            style={{backgroundColor:this.state.color}}
            onClick={this.toggleColorPicker.bind(this)} />

          {this.state.isColorPickerOpen &&
            <SwatchesPicker onChange={this.onPick.bind(this)} />
          }
        </dd>
      </dl>
    );
  }

}
