import React, { Component } from 'react';

const ICONS = {
  TEXT_PLAIN   : 'icon_text.png',
  TEXT_TEIXML  : 'icon_tei.png',
  IMAGE_UPLOAD : 'icon_image.png',
  IMAGE_IIIF   : 'icon_iiif.png'
};

export default class DocumentRow extends Component {

  render() {
    const type = this.props.filetypes[0];
    const label = (this.props.author) ?
      `${this.props.author}, ${this.props.title}` : this.props.title;
    const url = `/document/${this.props.id}/part/1/edit`;

    return (
      <div
        style={this.props.style}
        className="row">
        <a href={url} className={`type icon/* ${type}`}>
          <img src={`/assets/images/${ICONS[type]}`} />
        </a>
        <a href={url} className="document">{label}</a>
        <a href={url} className="language">{this.props.language ? this.props.language.toUpperCase() : ''}</a>
        <a href={url} className="date">{this.props.date}</a>
        <a href={url} className="uploaded">
        {new Intl.DateTimeFormat('en-GB', {
          year : 'numeric',
          month: 'short',
          day  : '2-digit'
        }).format(new Date(this.props.uploaded))}
        </a>
        <a href={url} className="last-edit">
          {this.props.lastedit && <TimeAgo date={this.props.last_edit} /> }
        </a>
      </div>
    )
  }

}
