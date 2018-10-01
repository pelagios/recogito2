import React, { Component } from 'react';

const ICONS = {
  TEXT_PLAIN   : 'icon_text.png',
  TEXT_TEI     : 'icon_tei.png',
  IMAGE_UPLOAD : 'icon_image.png',
  IMAGE_IIIF   : 'icon_iiif.png'
};

export default class DocumentRow extends Component {

  render() {
    const type = this.props.filetypes[0];
    const label = (this.props.author) ?
      `${this.props.author}, ${this.props.title}` : this.props.title;

    return (
      <div
        style={this.props.style}
        className="row">
        <a href="#" className={`type icon ${type}`}>
          <img src={`/public/images/${ICONS[type]}`} />
        </a>
        <a href="#" className="document">{label}</a>
        <a href="#" className="language">{this.props.language ? this.props.language.toUpperCase() : ''}</a>
        <a href="#" className="date">{this.props.date}</a>
        <a href="#" className="uploaded">
        {new Intl.DateTimeFormat('en-GB', {
          year : 'numeric',
          month: 'short',
          day  : '2-digit'
        }).format(new Date(this.props.uploaded))}
        </a>
        <a href="#" className="last-edit">
          {this.props.lastedit && <TimeAgo date={this.props.last_edit} /> }
        </a>
      </div>
    )
  }

}
