'use strict';

import React from 'react';
import ClassNames from 'classnames';
import TableStore from '../../lib/table-store';
import TableRow from './row';

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
      this.props.store.setSorting(this.state.sortingField, this.state.sortingOrder);
    });
  }

  render() {
    let renderColumn = (column, index) => {
      let classnames = ClassNames({
        'fc-table-th': true,
        'sorting': true,
        'sorting-desc': (this.state.sortingField === column.field) && this.state.sortingOrder,
        'sorting-asc': (this.state.sortingField === column.field) && !this.state.sortingOrder
      });
      return (
        <div
          className={classnames}
          key={`${index}-${column.field}`}
          onClick={this.onHeaderItemClick.bind(this, column.field)}
          >
          {column.title}
        </div>
      );
    };

    return (
      <TableRow>
        {this.props.store.columns.map(renderColumn)}
      </TableRow>
    );
  }
}

TableHead.propTypes = {
  store: React.PropTypes.instanceOf(TableStore)
};
