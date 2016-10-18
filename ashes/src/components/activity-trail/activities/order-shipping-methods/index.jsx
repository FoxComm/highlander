
import React from 'react';
import types from '../base/types';

import CordTarget from '../base/cord-target';
import Title from '../base/title';

const representatives = {
  [types.CART_SHIPPING_METHOD_UPDATED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>changed the shipping method</strong> on <CordTarget cord={cord} />
          &nbsp;to {cord.shippingMethod.name}
        </Title>
      );
    },
  },
  [types.CART_SHIPPING_METHOD_REMOVED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>removed the shipping method</strong> from <CordTarget cord={cord} />
        </Title>
      );
    },
  },
};

export default representatives;
