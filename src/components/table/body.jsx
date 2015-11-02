'use strict';

import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';

const TableBody = (props) => {
  const renderRow = props.renderRow || ((row) => (
    <TableRow>
      {props.columns.map((column) => <TableCell>{row[column.field]}</TableCell>)}
    </TableRow>
  ));
  return (
    <tbody className="fc-table-tbody">
      {props.rows.map(props.renderRow)}
    </tbody>
  );
};

TableBody.propTypes = {
  columns: PropTypes.array.isRequired,
  rows: PropTypes.array.isRequired,
  renderRow: PropTypes.func
};

export default TableBody;
