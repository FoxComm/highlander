
// libs
import React from 'react';
import types from '../base/types';
import { stateTitles } from '../../../../paragons/order';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';

const representatives = {
  [types.ORDER_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed the order state</strong> to {data.order.stateTitle}
          &nbsp;on <OrderTarget order={data.order}/>.
        </span>
      );
    },
    details: data => {
      return {
        newOne: data.order.stateTitle,
        previous: stateTitles[data.oldState] || data.oldState,
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
      const orders = data.orderRefNums.map(ref => {
        return <OrderLink key={ref} order={{title: 'Order', referenceNumber: ref}} />;
      });

      return (
        <span>
          <strong>changed the order state</strong> to {stateTitles[data.newState]} on orders {joinEntities(orders)}.
        </span>
      );
    },
  },
  [types.ORDER_REMORSE_PERIOD_INCREASED]: {
    title: data => {
      return (
        <span>
          <strong>increased remorse period for</strong> <OrderTarget order={data.order}/>.
        </span>
      );
    }
  },
};



export default representatives;
