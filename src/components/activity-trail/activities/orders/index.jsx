
// libs
import React from 'react';
import types from '../base/types';
import { statusTitles } from '../../../../paragons/order';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';

const representatives = {
  [types.ORDER_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed the order state</strong> to {data.order.statusTitle}
          &nbsp;on <OrderTarget order={data.order}/>.
        </span>
      );
    },
    details: data => {
      return {
        newOne: data.order.statusTitle,
        previous: statusTitles[data.oldState] || data.oldState,
      };
    }
  },
  [types.CART_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>created new</strong> <OrderTarget order={data.order}/>.
        </span>
      );
    },
  },
  [types.ORDER_BULK_STATE_CHANGED]: {
    title: data => {
      const orders = data.orders.map(ref => <OrderLink order={{title: 'Order', referenceNumber: ref}} />);

      return (
        <span>
          <strong>changed the order state</strong> to {statusTitles[data.newState]} on orders {joinEntities(orders)}.
        </span>
      );
    },
  },
};



export default representatives;
