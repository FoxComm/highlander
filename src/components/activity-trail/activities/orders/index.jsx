
// libs
import React from 'react';
import types from '../base/types';
import { stateTitles } from '../../../../paragons/order';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Title from '../base/title';

const representatives = {
  [types.ORDER_STATE_CHANGED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed the order state</strong> to {data.order.stateTitle}
          &nbsp;on <OrderTarget order={data.order}/>
        </Title>
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
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created new</strong> <OrderTarget order={data.order}/>
        </Title>
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
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>increased remorse period</strong> for <OrderTarget order={data.order}/>
        </Title>
      );
    }
  },
};



export default representatives;
