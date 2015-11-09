'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';
import { Moment, Date, DateTime, Time } from '../common/datetime';
import Currency from '../common/currency';
import Status from '../common/status';

const TableBody = (props) => {
  const renderCell = (cell, column) => {
    switch (column.type) {
      case 'id':
        return <Link to={column.model} params={{[column.model]: cell}}>{cell}</Link>;
      case 'image':
        return <img src={cell}/>;
      case 'status':
        return <Status value={cell} model={column.model}/>;
      case 'currency':
        return <Currency value={cell}/>;
      case 'moment':
        return <Moment value={cell}/>;
      case 'date':
        return <Date value={cell}/>;
      case 'datetime':
        return <DateTime value={cell}/>;
      case 'time':
        return <Time value={cell}/>;
      default:
        return cell;
    }
  };

  const renderRow = props.renderRow || ((row, index) => (
      <TableRow key={`${index}`}>
        {props.columns.map((column) => <TableCell>{renderCell(row[column.field], column)}</TableCell>)}
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
