import React, { PropTypes } from 'react';
import _ from 'lodash';

import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import Status from '../common/status';
import TableCell from '../table/cell';
import TableRow from '../table/row';

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

const OrderRow = props => {
  const { order, columns, ...rest } = props;
  const key = `order-${order.referenceNumber}`;

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `${key}-${col.field}`;
    let cellContents = null;

    switch (col.field) {
      case 'referenceNumber':
        cellContents = order.referenceNumber;
        break;
      case 'placedAt':
        cellContents = order.placedAt ? <DateTime value={order.placedAt} /> : '';
        break;
      case 'customer.name':
        cellContents = order.customer.name;
        break;
      case 'customer.email':
        cellContents = order.customer.email;
        break;
      case 'status':
        cellContents = <Status value={order.status} model="order" />;
        break;
      case 'shipping.status':
        const shippingStatus = compileShippingStatus(order);
        cellContents = shippingStatus
          ? <Status value={shippingStatus} model="shipment" />
          : '';
        break;
      case 'grandTotal':
        cellContents = order.grandTotal;
        break;
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellContents = <Checkbox />;
        break;
      default:
        return visibleCells;
    }

    visibleCells.push(
      <TableCell key={cellKey}>
        {cellContents}
      </TableCell>
    );

    return visibleCells;
  }, []);

  return (
    <TableRow {...rest}>
      {cells}
    </TableRow>
  );
};

OrderRow.propTypes = {
  order: PropTypes.object,
  columns: PropTypes.array
};

export default OrderRow;
