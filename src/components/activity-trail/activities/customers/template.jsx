
import _ from 'lodash';
import React from 'react';
import { Link } from '../../../link';

export default function makeCustomerDesc(message) {
  return {
    title: data => {
      const customerLink = (
        <Link className="fc-activity__link" to="customer" params={{customerId: data.customer.id}}>
          {data.customer.name}
        </Link>
      );

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
