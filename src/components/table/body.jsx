'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';


const TableBody = (props) => {
  const renderRow = props.renderRow || ((row, index) => (
      <TableRow key={`${index}`}>
        {props.columns.map((column) => <TableCell column={column}>{row[column.field]}</TableCell>)}
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
