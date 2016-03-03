import React from 'react';
import types from '../base/types';

import Title from '../base/title';
import AddressDetails from '../../../addresses/address-details';

function omitAddressName(address) {
  return {
    ...address,
    name: null
  };
}

const representatives = {
  [types.CUSTOMER_ADDRESS_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created a new address</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <AddressDetails address={omitAddressName(data.address)} />,
        previous: null,
      };
    },
  },
  [types.CUSTOMER_ADDRESS_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>edited an address</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <AddressDetails address={omitAddressName(data.oldInfo)} />,
        previous: <AddressDetails address={omitAddressName(data.newInfo)} />,
      };
    },
  },
  [types.CUSTOMER_ADDRESS_DELETED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed an address</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: null,
        previous: <AddressDetails address={omitAddressName(data.address)} />,
      };
    },
  },
};

export default representatives;
