
import React from 'react';
import moment from 'moment';
import { stateTitles } from 'paragons/order';

import Currency from 'ui/currency';
import { Link } from 'react-router';

function convertOrderData(orderDetails) {
  if (!orderDetails.grandTotal) {
    return {
      ...orderDetails,
      grandTotal: orderDetails.total,
      currency: 'USD',
      state: orderDetails.orderState,
    };
  }

  return orderDetails;
}

const OrderRow = props => {
  const { showDetailsLink } = props;
  const order = convertOrderData(props.order);

  let detailsColumn = null;
  if (showDetailsLink) {
    detailsColumn = (
      <td>
        <Link to={`/profile/orders/${order.referenceNumber}`}>View Details</Link>
      </td>
    );
  }

  return (
    <tr key={order.referenceNumber}>
      <td>
        {moment(order.placedAt).format('L')}
      </td>
      <td>{order.referenceNumber}</td>
      <td>
        <Currency value={order.grandTotal} currency={order.currency} />
      </td>
      <td>
        <span>{stateTitles[order.state]}</span>
      </td>
      <td>
        TRACKING ID
      </td>
      {detailsColumn}
    </tr>
  );
};

export default OrderRow;
