import React, { Component } from 'react';

export default class Body extends Component {

  /** Returns a suitable screen label for the body, based on its type **/
  getLabel(quote, body) {
    const t = body.type; // Shorthand
    const uri = (body.uri) ? this.props.uriParser(body.uri) : null;

    if (t == 'TAG')
      return value;
    else if (t == 'COMMENT')
      return `\u8220${body.value.substr(0, 30)}`;
    else if (t == 'PERSON' || t == 'EVENT')
      return quote;
    else if (t == 'PLACE')
      return (uri) ?
        ((uri.shortcode) ? `${uri.shortcode}:${uri.id}` : uri): 
        quote;
    else
      return null;
  }

  render() {
    return(
      <div className={`body ${this.props.data.type}`}>
        {this.getLabel(this.props.quote, this.props.data)}
      </div>
    )
  }

}
