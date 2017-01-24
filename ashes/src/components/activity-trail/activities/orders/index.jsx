
// libs
import React from 'react';
import types from '../base/types';
import { stateTitles } from '../../../../paragons/order';
import { joinEntities } from '../base/utils';

// components
import CordTarget from '../base/cord-target';
import CordLink from '../base/cord-link';
import Title from '../base/title';

const representatives = {
  [types.ORDER_STATE_CHANGED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;

      return (
        <Title activity={activity}>
          <strong>changed the order state</strong> to {cord.stateTitle}
          &nbsp;on <CordTarget cord={cord}/>
        </Title>
      );
    },
    details: data => {
      const order = data.order || data.cart;

      return {
        newOne: order.stateTitle,
        previous: stateTitles[data.oldState] || data.oldState,
      };
    }
  },
  [types.CART_CREATED]: {
    title: (data, activity) => {
      const cart = { ...data.cart, isCart: true};
      return (
        <Title activity={activity}>
          <strong>created new</strong> <CordTarget cord={cart}/>
        </Title>
      );
    },
  },
  [types.ORDER_BULK_STATE_CHANGED]: {
    title: data => {
      const orders = data.cordRefNums.map(ref => {
        return <CordLink key={ref} cord={{referenceNumber: ref}} />;
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
      const cord = data.order || data.cart;

      return (
        <Title activity={activity}>
          <strong>increased remorse period</strong> for <CordTarget cord={cord}/>
        </Title>
      );
    }
  },
};



export default representatives;
