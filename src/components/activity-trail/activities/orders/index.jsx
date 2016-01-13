
import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';
import { statusTitles } from '../../../../paragons/order';

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
};

export default representatives;
