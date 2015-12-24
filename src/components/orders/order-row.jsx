import React from 'react';
import _ from 'lodash';

import { DateTime } from '../common/datetime';
import Currency from '../common/currency';
import Link from '../link/link';
import Status from '../common/status';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const OrderRow = props => {
  const { order, columns, ...rest } = props;
  const key = `order-${order.referenceNumber}`;

  const cells = _.reduce(columns, (visibleCells, col) => {
    switch (col.field) {
      case 'referenceNumber':
        visibleCells.push(
          <TableCell>
            <Link to={'order'} params={{order: order.referenceNumber}}>
              {order.referenceNumber}
            </Link>
          </TableCell>
        );
        break;
      case 'placedAt':
        visibleCells.push(
          <TableCell>
            {order.placedAt && <DateTime value={order.placedAt} />}
          </TableCell>
        );
        break;
      case 'customer.name':
        visibleCells.push(
          <TableCell>
            {order.customer.name}
          </TableCell>
        );
        break;
      case 'customer.email':
        visibleCells.push(
          <TableCell>
            {order.customer.email}
          </TableCell>
        );
        break;
      case 'status':
        visibleCells.push(
          <TableCell>
            <Status value={order.status} model={"order"}/>
          </TableCell>
        );
        break;
      case 'payment.status':
        visibleCells.push(
          <TableCell>
            <Status value={order.paymentStatus} model={"payment"}/>
          </TableCell>
        );
        break;
      case 'shipping.status':
        visibleCells.push(
          <TableCell>
            <Status value={order.shippingStatus} model={"shipment"}/>
          </TableCell>
        );
        break;
      case 'grandTotal':
        visibleCells.push(
          <TableCell>
            <Currency value={order.grandTotal}/>
          </TableCell>
        );
        break;
    }

    return visibleCells;
  }, []);

  return (
    <TableRow key={key} {...rest}>
      {cells}
    </TableRow>
  );
};

export default OrderRow;
