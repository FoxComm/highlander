import React, { PropTypes } from 'react';
import { Moment, Date, DateTime, Time } from '../common/datetime';
import Currency from '../common/currency';
import Status from '../common/status';
import Link from '../link/link';

const TableCell = props => {
  const render = (cell, column = {}) => {
    switch (column.type) {
      case 'id':
        return <Link to={column.model} params={{[column.model]: cell}}>{cell}</Link>;
      case 'image':
        return <img src={cell}/>;
      case 'status':
        return <Status value={cell} model={column.model}/>;
      case 'currency':
        return <Currency value={cell}/>;
      case 'transaction':
        return <Currency value={cell} isTransaction={true} />;
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
  return (
    <td className="fc-table-td" colSpan={props.colspan}>
      {render(props.children, props.column)}
    </td>
  );
};

TableCell.propTypes = {
  children: PropTypes.node,
  colspan: PropTypes.number,
  column: PropTypes.object
};

export default TableCell;
