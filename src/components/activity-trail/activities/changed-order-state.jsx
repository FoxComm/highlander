
import React, { PropTypes } from 'react';
import OrderTarget from './base/order-target';
import Activity from './base/activity';

export default function({activity}) {
  const { order } = activity.data;

  const actionDescription = (
    <div>
      {activity.data.author} <strong>changed the order state</strong> to {order.statusTitle}
      &nbsp;on <OrderTarget order={order} />.
    </div>
  );

  const params = {
    activity,
    actionDescription
  };

  return <Activity {...params} />;
}
