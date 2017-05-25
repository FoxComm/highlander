/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from '../../table/multi-select-row';

type Props = {
  order: Object,
  columns: Columns,
  params: Object,
};

const OrderTransactionRow = (props: Props) => {
  const { order, columns, params } = props;

  const computePaymentState = (order: Object) => {
    // This is a pretty limited algorithm, but it matches what Phoenix is doing.
    // We'll beef it up when we get shipping and payment capture in the system.
    const authorizations = _.reduce(order.payments, (result, payment) => {
      const { paymentMethodType } = payment;
      if ((paymentMethodType == 'creditCard' && payment.creditCardState == 'fullCapture') ||
        (paymentMethodType == 'giftCard' && payment.giftCardState == 'capture') ||
        (paymentMethodType == 'storeCredit' && payment.storeCreditState == 'capture')) {
        return result + 1;
      }

      return result;
    }, 0);

    return authorizations > 0 && authorizations == order.payments.length ? 'Full Capture' : 'Auth';
  };

  const setCellContents = (order: Object, field: string) => {
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
          return _.last(order.assignees).name;
        }
        break;
      case 'payment.state':
        return computePaymentState(order);
      default:
        return null;
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="order"
      linkParams={{order: order.referenceNumber}}
      row={order}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default OrderTransactionRow;
