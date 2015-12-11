
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
    <div>
      {data.author} <strong>edited the shipping address</strong> on <OrderTarget order={data.order} />.
    </div>
  );
};

export const details = data => {
  return {
    newOne:  <AddressDetails address={removeName(data.newAddress)} />,
    previous: <AddressDetails address={removeName(data.previousAddress)} />,
  };
};
