import React, { PropTypes } from 'react';
import { Moment, Date, DateTime, Time } from '../common/datetime';
import Currency from '../common/currency';
import Status from '../common/status';
import Link from '../link/link';

import _ from 'lodash';

const TableCell = props => {
  const { children, colspan, column, ...rest } = props;

  let cell = null;

  if (!_.isNull(children)) {
    const type = _.get(column, 'type', '');
    switch (type) {
      case 'id':
        cell = <Link to={column.model} params={{[column.model]: children}}>{children}</Link>;
        break;
      case 'image':
        cell = <img src={children}/>;
        break;
      case 'status':
        cell = <Status value={children} model={column.model}/>;
        break;
      case 'currency':
        cell = <Currency value={children}/>;
        break;
      case 'transaction':
        cell = <Currency value={children} isTransaction={true} />;
        break;
      case 'moment':
        cell = <Moment value={children}/>;
        break;
      case 'date':
        cell = <Date value={children}/>;
        break;
      case 'datetime':
        cell = <DateTime value={children}/>;
        break;
      case 'time':
        cell = <Time value={children}/>;
        break;
      default:
        cell = children;
        break;
    }
  }

  return <td className="fc-table-td" colSpan={colspan} {...rest}>{cell}</td>;
};

TableCell.propTypes = {
  children: PropTypes.node,
  colspan: PropTypes.number,
  column: PropTypes.object
};

export default TableCell;
