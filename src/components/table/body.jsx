'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';

const TableBody = (props) => {
  const renderRow = props.renderRow || ((row, idx) => (
    <TableRow key={`row-${idx}`}>
      {props.columns.map((column, idx) => <TableCell key={`cell-${idx}`}>{row[column.field]}</TableCell>)}
    </TableRow>
  ));

  return (
    <tbody className="fc-table-tbody">
      {_.flatten(props.rows.map(renderRow))}
    </tbody>
  );
};

TableBody.propTypes = {
  columns: PropTypes.array.isRequired,
  rows: PropTypes.array.isRequired,
  renderRow: PropTypes.func
};

export default TableBody;
