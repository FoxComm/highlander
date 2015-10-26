'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';
import TableRow from './row';

export default class TableHead extends React.Component {

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      sortingField: this.props.data.sortingField,
      sortingOrder: this.props.data.sortingOrder
    };
  }

  onHeaderItemClick(field, event) {
    event.preventDefault();
    this.setState({
      sortingField: field,
      sortingOrder: (field === this.state.sortingField) ? !this.state.sortingOrder : true
    }, () => {
      this.props.data.setSorting(this.state.sortingField, this.state.sortingOrder);
    });
  }

  render() {
    let renderColumn = (column, index) => {
      let classnames = classNames({
        'fc-table-th': true,
        'sorting': true,
        'sorting-desc': (this.state.sortingField === column.field) && this.state.sortingOrder,
        'sorting-asc': (this.state.sortingField === column.field) && !this.state.sortingOrder
      });
      return (
        <th
          className={classnames}
          key={`${index}-${column.field}`}
          onClick={this.onHeaderItemClick.bind(this, column.field)}
          >
          {column.title}
        </th>
      );
    };

    return (
      <thead>
        <TableRow>
          {this.props.data.columns.map(renderColumn)}
        </TableRow>
      </thead>
    );
  }
}
