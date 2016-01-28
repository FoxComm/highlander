import React from 'react';
import types from '../base/types';

import CustomerLink from '../base/customer-link';
import AddressDetails from '../../../addresses/address-details';

function omitAddressName(address) {
  return {
    ...address,
    name: null
  };
}

const representatives = {
  [types.CUSTOMER_ADDRESS_CREATED]: {
    title: (data, {context}) => {
      const targetSense = context.userType == 'admin' ?
        <span> for customer <CustomerLink customer={data.customer} /></span> : null;

      return (
        <span>
          <strong>created a new address</strong>{targetSense}.
        </span>
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
    title: data => {
      return (
        <span>
        <strong>edited an address</strong> for <CustomerLink customer={data.customer} />.
      </span>
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
    title: data => {
      return (
        <span>
          <strong>removed an address</strong> on customer <CustomerLink customer={data.customer} />.
        </span>
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
