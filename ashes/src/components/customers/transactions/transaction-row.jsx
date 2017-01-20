import React, { PropTypes } from 'react';
import _ from 'lodash';

import MultiSelectRow from '../../table/multi-select-row';

const computePaymentState = order => {
  // This is a pretty limited algorithm, but it matches what Phoenix is doing.
  // We'll beef it up when we get shipping and payment capture in the system.
  const authorizations = _.reduce(order.payments, (result, payment) => {
    const { paymentMethodType } = payment;
    if ((paymentMethodType == 'creditCard' && payment.creditCardState == 'fullCapture') ||
      (paymentMethodType == 'giftCard' && payment.giftCardState == 'fullCapture') ||
      (paymentMethodType == 'storeCredit' && payment.storeCreditState == 'fullCapture')) {
      return result + 1;
    }

    return result;
  }, 0);

  return authorizations > 0 && authorizations == order.payments.length ? 'Full Capture' : 'Auth';
};

const setCellContents = (order, field) => {
  switch (field) {
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


const OrderTransactionRow = (props) => {
  const { order, columns, params } = props;
  const key = `order-${order.referenceNumber}`;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="order"
      linkParams={{order: order.referenceNumber}}
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

export default OrderTransactionRow;
