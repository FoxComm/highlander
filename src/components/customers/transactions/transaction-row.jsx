import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../../route-helpers';

import MultiSelectRow from '../../table/multi-select-row';

const computePaymentState = order => {
  // This is a pretty limited algorithm, but it matches what Phoenix is doing.
  // We'll beef it up when we get shipping and payment capture in the system.
  const authorizations = _.reduce(order.payments, (result, payment) => {
    const { paymentMethodType } = payment;
    if ((paymentMethodType == 'creditCard' && payment.creditCardState == 'auth') ||
        (paymentMethodType == 'giftCard' && payment.giftCardState == 'auth') ||
        (paymentMethodType == 'storeCredit' && payment.storeCreditState == 'auth')) {
      return result + 1;
    }
    
    return result;
  }, 0);

  return authorizations > 0 && authorizations == order.payments.length ? 'Auth' : 'Cart';
};

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
    case 'payment.state':
      return computePaymentState(order);
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
