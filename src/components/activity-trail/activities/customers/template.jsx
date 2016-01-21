
import _ from 'lodash';
import React from 'react';

import CustomerLink from './../base/customer-link';

export default function makeCustomerDesc(message) {
  return {
    title: data => {
      const customerLink = <CustomerLink customer={data.customer} />;

      if (_.isString(message)) {
        return (
          <span>
            <strong>{message}</strong> customer {customerLink}.
          </span>
        );
      } else {
        return (
          <span>
            {message(customerLink)}
          </span>
        );
      }
    }
  };
}
