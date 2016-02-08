import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../../route-helpers';

import { DateTime } from '../../common/datetime';
import { Checkbox } from '../../checkbox/checkbox';
import Currency from '../../common/currency';
import Link from '../../link/link';
import MultiSelectRow from '../../table/multi-select-row';


const setCellContents = (order, field) => {
  switch(field) {
    case 'referenceNumber':
    case 'placedAt':
    case 'customer.modality':
    case 'state':
    case 'grandTotal':
      return _.get(order, field);
    case 'return':
      if (!_.isEmpty(order.rmas)) {
        return _.last(order.rmas).referenceNumber;
      }
      break;
    case 'assignee':
      if (!_.isEmpty(order.assignees)) {
        return _.last(order.assignees).firstName;
      }
      break;
    default:
      return null;
  }
};


const OrderTransactionRow = (props, context) => {
  const { order, columns, params } = props;
  const key = `order-${order.referenceNumber}`;
  const clickAction = () => {
    transitionTo(context.history, 'order', { order: order.referenceNumber });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={order}
      setCellContents={setCellContents}
      params={params} />
  );
};

OrderTransactionRow.propTypes = {
  order: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
};

OrderTransactionRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default OrderTransactionRow;
