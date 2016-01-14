
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import AddressDetails from '../../../addresses/address-details';

function omitAddressName(address) {
  return {
    ...address,
    name: null
  };
}

const representatives = {
  [types.ORDER_SHIPPING_ADDRESS_UPDATED]: {
    title: data => {
      return (
        <span>
          <strong>edited the shipping address</strong> on <OrderTarget order={data.order} />.
        </span>
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
    title: data => {
      return (
        <span>
          <strong>added the shipping address</strong> on <OrderTarget order={data.order} />.
        </span>
      );
    },
    details: data => {
      return {
        newOne: <AddressDetails address={omitAddressName(data.address)} />,
        previous: null,
      };
    }
  },
  [types.ORDER_SHIPPING_ADDRESS_REMOVED]: {
    title: data => {
      return (
        <span>
          <strong>removed the shipping address</strong> on <OrderTarget order={data.order} />.
        </span>
      );
    },
    details: data => {
      return {
        newOne: null,
        previous: <AddressDetails address={omitAddressName(data.address)} />,
      };
    }
  },
};

export default representatives;
