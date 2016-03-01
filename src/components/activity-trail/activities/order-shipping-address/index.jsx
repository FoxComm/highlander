
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import Title from '../base/title';
import AddressDetails from '../../../addresses/address-details';

function omitAddressName(address) {
  return {
    ...address,
    name: null
  };
}

const representatives = {
  [types.ORDER_SHIPPING_ADDRESS_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>edited a shipping address</strong> on <OrderTarget order={data.order} />
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <AddressDetails address={omitAddressName(data.order.shippingAddress)} />,
        previous: <AddressDetails address={omitAddressName(data.address)} />,
      };
    }
  },
  [types.ORDER_SHIPPING_ADDRESS_ADDED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>added the shipping address</strong> to <OrderTarget order={data.order} />
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
  [types.ORDER_SHIPPING_ADDRESS_REMOVED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed the shipping address</strong> from <OrderTarget order={data.order} />
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
