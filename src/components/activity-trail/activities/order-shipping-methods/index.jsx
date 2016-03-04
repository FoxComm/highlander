
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import Title from '../base/title';

const representatives = {
  [types.ORDER_SHIPPING_METHOD_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <stong>changed the shipping method</stong> on <OrderTarget order={data.order} />
          &nbsp;to {data.order.shippingMethod.name}
        </Title>
      );
    },
  },
  [types.ORDER_SHIPPING_METHOD_REMOVED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <stong>removed the shipping method</stong> from <OrderTarget order={data.order} />
        </Title>
      );
    },
  },
};

export default representatives;
