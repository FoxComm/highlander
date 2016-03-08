// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import columnPropType from './column-prop-type';
import { Moment, Date, DateTime, Time } from '../common/datetime';
import Currency from '../common/currency';
import State from '../common/state';
import OriginType from '../common/origin-type';
import Link from '../link/link';


function getCell(column, children) {
  const type = _.get(column, 'type');
  switch (type) {
    case 'id':
      return <Link to={column.model} params={{[column.model]: children}}>{children}</Link>;
    case 'image':
      return <img src={children} />;
    case 'state':
      return <State value={children} model={column.model} />;
    case 'currency':
      return <Currency value={children} />;
    case 'transaction':
      return <Currency value={children} isTransaction={true} />;
    case 'moment':
      return <Moment value={children} />;
    case 'date':
      return <Date value={children} />;
    case 'datetime':
      return <DateTime value={children} />;
    case 'time':
      return <Time value={children} />;
    default:
      return children;
  }
}

const TableBodyCell = props => {
  const { children, colspan, column, ...rest } = props;

  const cell = _.isNull(children) ? null : getCell(column, children);

  return <td className="fc-table-td" colSpan={colspan} {...rest}>{cell}</td>;
};

TableBodyCell.propTypes = {
  colspan: PropTypes.number,
  column: columnPropType,
  children: PropTypes.node,
  colspan: PropTypes.number,
};

export default TableBodyCell;
