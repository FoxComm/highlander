'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';
import TableRow from './row';

const TableHead = (props) => {
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
      {props.columns.map(renderColumn)}
    </TableRow>
    </thead>
  );
};

TableHead.propTypes = {
  columns: PropTypes.array.isRequired
};

export default TableHead;
