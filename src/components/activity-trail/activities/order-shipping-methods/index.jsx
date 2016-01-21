
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';

const representatives = {
  [types.ORDER_SHIPPING_METHOD_UPDATED]: {
    title: data => {
      return (
        <span>
          <stong>changed the shipping method</stong> on <OrderTarget order={data.order} />
          &nbsp;to {data.order.shippingMethod.name}.
        </span>
      );
    },
  },
  [types.ORDER_SHIPPING_METHOD_REMOVED]: {
    title: data => {
      return (
        <span>
          <stong>removed the shipping method</stong> from <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
};

export default representatives;
