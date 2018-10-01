import React, { Component } from 'react';
import { SortableContainer, SortableElement, arrayMove } from 'react-sortable-hoc';

const SortableItem = SortableElement(({ label }) =>
  <div className="card">{label}</div>
);

const SortableList = SortableContainer(({ items }) =>
  <div className="column-order">
    {items.map((item, idx) => (
      <SortableItem key={`card-${idx}`} index={idx} label={item.label} />
    ))}
  </div>
);

export default class ColumnOrder extends Component {

  constructor(props) {
    super(props);
    this.state = { items: props.items };
  }

  componentWillReceiveProps(next) {
    // First, remove those items that are not included in the update
    let updatedItems = this.state.items.filter(item => next.items.indexOf(item) > -1);

    // Then append items that are new to the end of the array
    const toAppend = next.items.filter(item => this.state.items.indexOf(item) == -1);
    updatedItems = updatedItems.concat(toAppend);

    this.setState({ items: updatedItems });
  }

  onSortEnd(evt) {
    this.setState({
      items: arrayMove(this.state.items, evt.oldIndex, evt.newIndex)
    }, () => {
      if (this.props.onSortEnd)
        this.props.onSortEnd(this.state.items);
    });
  }

  render() {
    return (
      <SortableList
        items={this.state.items}
        onSortEnd={this.onSortEnd.bind(this)}/>
    )
  }

}
