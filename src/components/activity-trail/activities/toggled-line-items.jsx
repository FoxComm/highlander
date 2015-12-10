
import React, { PropTypes } from 'react';
import OrderTarget from './base/order-target';
import Activity from './base/activity';
import types from './base/types';

const titles = {
  [types.ADDED_LINE_ITEMS]: 'added',
  [types.REMOVED_LINE_ITEMS]: 'removed',
};

export default function(type) {
  const title = titles[type];

  return ({activity}) => {
    const data = activity.data;

    const actionDescription = (
      <div>
        {data.author} <strong>{title} {data.quantity} of {data.name}</strong> on <OrderTarget order={data.order} />.
      </div>
    );

    const params = {
      activity,
      actionDescription
    };
    return <Activity {...params} />;
  };
}
