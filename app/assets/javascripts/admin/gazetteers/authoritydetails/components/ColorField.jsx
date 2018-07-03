import React, { Component } from 'react';
import { SwatchesPicker } from 'react-color';

export default class ColorField extends Component {

  render() {
    return (
      <dl>
        <dt><label for="color">Color</label></dt>
        <dd>
          {/* <SwatchesPicker /> */}
        </dd>
      </dl>
    );
  }

}
