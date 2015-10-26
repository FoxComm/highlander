'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';
import TableRow from './row';

export default class TableHead extends React.Component {
  static propTypes = {
    store: React.PropTypes.instanceOf(TableStore)
  };

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      sortingField: this.props.store.sortingField,
      sortingOrder: this.props.store.sortingOrder
    };
  }

  onHeaderItemClick(field, event) {
    event.preventDefault();
    this.setState({
      sortingField: field,
      sortingOrder: (field === this.state.sortingField) ? !this.state.sortingOrder : true
    }, () => {
      this.props.store.setSorting(this.state.sortingField, this.state.sortingOrder);
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
          {this.props.store.columns.map(renderColumn)}
        </TableRow>
      </thead>
    );
  }
}
