'use strict';

import React from 'react';

export default class TableHead extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  onHeaderItemClick(field, event) {
    event.preventDefault();
    this.setState({
      sortingField: field,
      sortingOrder: (field === this.state.sortingField) ? !this.state.sortingOrder : true
    }, () => {
      this.props.setSorting(this.state.sortingField, this.state.sortingOrder);
    });
  }

  render() {
    let createColumn = (column, idx) => {
      return (
        <th key={`${idx}-${column.field}`} onClick={this.onHeaderItemClick.bind(this, column.field)}>
          {column.text}
        </th>
      );
    };
    return <thead><tr>{this.props.columns.map(createColumn)}</tr></thead>;
  }
}

TableHead.propTypes = {
  columns: React.PropTypes.array,
  setSorting: React.PropTypes.func
};
