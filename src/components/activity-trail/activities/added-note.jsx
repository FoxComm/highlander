
import React, { PropTypes } from 'react';
import OrderTarget from './base/order-target';
import Activity from './base/activity';

export default function({activity}) {
  const actionDescription = (
    <div>
      {activity.data.author} <strong>added a note</strong> on <OrderTarget order={activity.data.order} />.
    </div>
  );

  const params = {
    activity,
    actionDescription
  };
  return <Activity {...params} />;
}
