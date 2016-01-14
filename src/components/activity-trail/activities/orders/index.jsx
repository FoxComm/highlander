
// libs
import React from 'react';
import types from '../base/types';
import { statusTitles } from '../../../../paragons/order';

// components
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
      return (
        <span>
          <strong>changed the order state</strong> to {statusTitles[data.newState]} on set of orders.
        </span>
      );
    },
    details: data => {
      return (
        <div>
          <div className="fc-activity__details-head">List of affected orders</div>
          <ul className="fc-activity__details-enum">{data.orders.map(ref => {
            return (
              <li>
                <OrderTarget order={{title: 'Order', referenceNumber: ref}} />
              </li>
            );
          })}</ul>
        </div>
      );
    }
  },
};



export default representatives;
