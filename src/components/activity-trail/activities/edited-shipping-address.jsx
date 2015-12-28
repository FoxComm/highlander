
import React from 'react';
import OrderTarget from './base/order-target';
import AddressDetails from '../../addresses/address-details';

const removeName = address => {
  return {
    ...address,
    name: null
  };
};

export const title = data => {
  return (
    <span>
      <strong>edited the shipping address</strong> on <OrderTarget order={data.order} />.
    </span>
  );
};

export const details = data => {
  return {
    newOne: <AddressDetails address={removeName(data.newAddress)} />,
    previous: <AddressDetails address={removeName(data.previousAddress)} />,
  };
};
