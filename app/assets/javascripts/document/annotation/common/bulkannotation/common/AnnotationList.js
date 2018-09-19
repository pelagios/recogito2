import React, { Component } from 'react';

export class AnnotationList {

  constructor(annotations) {
    this._annotations = annotations;
    this.length = annotations.length;
  }

  /** Helper: gets bodies of the given type from one annotation **/
  _getBodiesOfType(a, type) {
    return a.bodies.filter(b => {
      return b.type == type;
    });
  }

  /** Helper to filter by given body type and property (value or URI) **/
  _filterByBodyTypeAndProperty(type, prop, value) {
    const filtered = this._annotations.filter(a => {
      const bodies = this._getBodiesOfType(a, type);
      const values = bodies.map(b => {
        return (b[prop]) ? b[prop].toLowerCase() : null;
      });
      return values.includes(value.toLowerCase());
    });

    return new AnnotationList(filtered);
  }

  /** Filters annotations by whether they have at least on body of the requested type **/
  filterByBodyType(type) {
    const filtered = this._annotations.filter(a => {
      const matchingBodies = this._getBodiesOfType(a, type);
      return matchingBodies.length > 0;
    });
    return new AnnotationList(filtered);
  }

  filterByQuote(quote) {
    return this._filterByBodyTypeAndProperty('QUOTE', 'value', quote);
  }

  filterByEntityURI(uri) {
    return this._filterByBodyTypeAndProperty('PLACE', 'uri', uri);
  }

  filterByVerificationStatus(status) {
    const filtered = this._annotations.filter(a => {
      const placeBodies = this._getBodiesOfType(a, 'PLACE');
      const statusValues = placeBodies.map(b => {
        return (b.status) ? b.status.value : null;
      });

      return statusValues.includes(status);
    });

    return new AnnotationList(filtered);
  }

  /** Removes the annotation with the given annotation ID **/
  removeById(id) {
    const filtered = this._annotations.filter(a => {
      return a.annotation_id != id;
    });

    return new AnnotationList(filtered);
  }

}
