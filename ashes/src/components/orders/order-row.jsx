import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

const compileShippingStatus = order => {
  if (order.state == 'canceled') {
    return 'canceled';
  }

  let canceledItemCount = 0;
  let pendingItemCount = 0; // Status equals cart, ordered, (fraud|remorse|manual)Hold
  let partiallyShippedItemCount = 0;
  let shippedItemCount = 0;
  let deliveredItemCount = 0;

  _.forEach(order.shipments, shipment => {
    switch(shipment.state) {
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

  if (order.shipments.length > 0) {
    if (order.state == 'fulfillmentStarted' && shippedItemCount > 0 &&
        deliveredItemCount == 0 && (pendingItemCount > 0 || partiallyShippedItemCount > 0)) {
      return 'Partially Shipped';
    } else if (order.state == 'fulfillmentStarted' && deliveredItemCount > 0 &&
               (shippedItemCount > 0 || pendingItemCount > 0)) {
      return 'Partially Delivered';
    } else if (canceledItemCount == order.shipments.length) {
      return 'Canceled';
    } else if (shippedItemCount + canceledItemCount == order.shipments.length) {
      return 'Shipped';
    } else if (deliveredItemCount + canceledItemCount == order.shipments.length) {
      return 'Delivered';
    }
  }

  return null;
};

const setCellContents = (order, field) => {
  switch(field) {
    case 'referenceNumber':
    case 'placedAt':
    case 'customer.name':
    case 'customer.email':
    case 'state':
    case 'grandTotal':
      return _.get(order, field);
    case 'shipping.state':
      return compileShippingStatus(order);
    default:
      return null;
  }
};


const OrderRow = (props, context) => {
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

OrderRow.propTypes = {
  order: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

export default OrderRow;
