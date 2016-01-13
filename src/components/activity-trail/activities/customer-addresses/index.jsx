import React from 'react';
import types from '../base/types';

import CustomerLink from '../base/customer-link';

const representatives = {
  [types.CUSTOMER_ADDRESS_CREATED_BY_ADMIN]: {
    title: data => {
      return (
        <span>
          <strong>created new address</strong> for customer <CustomerLink customer={data.customer} />.
        </span>
      );
    }
  },
  [types.CUSTOMER_ADDRESS_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>created new address</strong>.
        </span>
      );
    }
  }
};

export default representatives;
