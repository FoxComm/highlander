
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import Title from '../base/title';

const representatives = {
  [types.ORDER_SHIPPING_METHOD_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed the shipping method</strong> on <OrderTarget order={data.order} />
          &nbsp;to {data.order.shippingMethod.name}
        </Title>
      );
    },
  },
  [types.ORDER_SHIPPING_METHOD_REMOVED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed the shipping method</strong> from <OrderTarget order={data.order} />
        </Title>
      );
    },
  },
};

export default representatives;
