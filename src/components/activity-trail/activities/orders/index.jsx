
import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';

const representatives = {
  [types.ORDER_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed the order state</strong> to {data.order.statusTitle}
          &nbsp;on <OrderTarget order={data.order}/>.
        </span>
      );
    }
  },
};

export default representatives;
