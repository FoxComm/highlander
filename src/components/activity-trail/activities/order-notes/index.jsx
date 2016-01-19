
import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';

const representatives = {
  [types.ORDER_NOTE_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>added a note</strong> on <OrderTarget order={data.order}/>.
        </span>
      );
    }
  },
};

export default representatives;
