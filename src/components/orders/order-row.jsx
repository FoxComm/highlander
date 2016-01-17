import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import Status from '../common/status';
import MultiSelectRow from '../table/multi-select-row';

const compileShippingStatus = order => {
  if (order.status == 'canceled') {
    return 'Canceled';
  }

  let canceledItemCount = 0;
  let pendingItemCount = 0; // Status equals cart, ordered, (fraud|remorse|manual)Hold
  let fulfillmentStartedItemCount = 0;
  let partiallyShippedItemCount = 0;
  let shippedItemCount = 0;
  let deliveredItemCount = 0;

  _.forEach(order.shipments, shipment => {
    switch(shipment.status) {
      case 'canceled':
        canceledItemCount += 1;
        break;
      case 'cart':
      case 'ordered':
      case 'fraudHold':
      case 'remorseHold':
      case 'manualHold':
        pendingItemCount += 1;
        break;
      case 'fulfillmentStarted':
        fulfillmentStartedItemCount += 1;
        break;
      case 'partiallyShipped':
        partiallyShippedItemCount += 1;
        break;
      case 'shipped':
        shippedItemCount += 1;
        break;
      case 'delivered':
        deliveredItemCount += 1;
        break;
    }
  });

  if (order.status == 'fulfillmentStarted' && shippedItemCount > 0 &&
      deliveredItemCount == 0 && (pendingItemCount > 0 || partiallyShippedItemCount > 0)) {
    return 'Partially Shipped';
  } else if (order.status == 'fulfillmentStarted' && deliveredItemCount > 0 &&
             (shippedItemCount > 0 || pendingItemCount > 0)) {
    return 'Partially Delivered';
  } else if (canceledItemCount == order.shipments.length) {
    return 'Canceled';
  } else if (shippedItemCount + canceledItemCount == order.shipments.length) {
    return 'Shipped';
  } else if (deliveredItemCount + canceledItemCount == order.shipments.length) {
    return 'Delivered';
  } else {
    return null;
  }
};

const setCellContents = (order, field) => {
  switch(field) {
    case 'referenceNumber':
      return order.referenceNumber;
    case 'placedAt':
      return order.placedAt;
    case 'customer.name':
      return order.customer.name;
    case 'customer.email':
      return order.customer.email;
    case 'status':
      return order.status;
    case 'shipping.status':
      return compileShippingStatus(order);
    case 'grandTotal':
      return order.grandTotal;
    default:
      return null;
  }
};
    

const OrderRow = (props, context) => {
  const { order, columns, ...rest } = props;
  const key = `order-${order.referenceNumber}`;
  const clickAction = () => {
    transitionTo(context.history, 'order', { order: order.referenceNumber });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      setCellContents={setCellContents} />
  );
};

OrderRow.propTypes = {
  order: PropTypes.object,
  columns: PropTypes.array
};

OrderRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default OrderRow;
