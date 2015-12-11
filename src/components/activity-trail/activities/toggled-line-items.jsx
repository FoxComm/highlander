
import React, { PropTypes } from 'react';
import OrderTarget from './base/order-target';
import Activity from './base/activity';
import types from './base/types';

const titles = {
  [types.ADDED_LINE_ITEMS]: 'added',
  [types.REMOVED_LINE_ITEMS]: 'removed',
};

export const title = (data, activity) => {
  const title = titles[activity.type];

  return (
    <div>
      {data.author} <strong>{title} {data.quantity} of {data.name}</strong> on <OrderTarget order={data.order} />.
    </div>
  );
};
