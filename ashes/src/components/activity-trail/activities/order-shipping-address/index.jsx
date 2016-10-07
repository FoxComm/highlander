
import React from 'react';
import types from '../base/types';

import CordTarget from '../base/cord-target';
import Title from '../base/title';
import AddressDetails from '../../../addresses/address-details';

function omitAddressName(address) {
  return {
    ...address,
    name: null
  };
}

const representatives = {
  [types.CART_SHIPPING_ADDRESS_UPDATED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;

      return (
        <Title activity={activity}>
          <strong>edited a shipping address</strong> on <CordTarget cord={cord} />
        </Title>
      );
    },
    details: data => {
      const order = data.order || data.cart;

      return {
        newOne: <AddressDetails address={omitAddressName(order.shippingAddress)} />,
        previous: <AddressDetails address={omitAddressName(data.address)} />,
      };
    }
  },
  [types.CART_SHIPPING_ADDRESS_ADDED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;

      return (
        <Title activity={activity}>
          <strong>added the shipping address</strong> to <CordTarget cord={cord} />
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <AddressDetails address={omitAddressName(data.shippingAddress)} />,
        previous: null,
      };
    }
  },
  [types.CART_SHIPPING_ADDRESS_REMOVED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;

      return (
        <Title activity={activity}>
          <strong>removed the shipping address</strong> from <CordTarget cord={cord} />
        </Title>
      );
    },
    details: data => {
      if (!data.shippingAddress) return null;

      return {
        newOne: null,
        previous: <AddressDetails address={omitAddressName(data.shippingAddress)} />,
      };
    }
  },
};

export default representatives;
