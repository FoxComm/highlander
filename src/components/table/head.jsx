'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';
import TableRow from './row';

export default class TableHead extends React.Component {
  render() {
    const renderColumn = (column, index) => {
      const classnames = classNames({
        'fc-table-th': true,
        'sorting': true
      });
      return (
        <th className={classnames} key={`${column.field}`}>
          {column.text}
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
