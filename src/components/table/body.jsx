'use strict';

import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';

const TableBody = (props) => {
  const renderRow = props.renderRow || ((row) => (
    <TableRow>
      {props.data.columns.map((column) => <TableCell>{row[column.field]}</TableCell>)}
    </TableRow>
  ));
  return (
    <tbody className="fc-table-tbody">
      {props.data.rows.map(props.renderRow)}
    </tbody>
  );
};

TableBody.propTypes = {
  renderRow: PropTypes.func
};

export default TableBody;
