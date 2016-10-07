
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import Title from '../base/title';

const representatives = {
  [types.CART_SHIPPING_METHOD_UPDATED]: {
    title: (data, activity) => {
      const order = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>changed the shipping method</strong> on <OrderTarget order={order} />
          &nbsp;to {order.shippingMethod.name}
        </Title>
      );
    },
  },
  [types.CART_SHIPPING_METHOD_REMOVED]: {
    title: (data, activity) => {
      const order = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>removed the shipping method</strong> from <OrderTarget order={order} />
        </Title>
      );
    },
  },
};

export default representatives;
