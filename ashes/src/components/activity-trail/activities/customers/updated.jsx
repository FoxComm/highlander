
import React from 'react';
import { Link } from '../../../link';
import CustomerInfo from './customer-info';

export const title = data => {
  return (
    <span>
      <strong>edited the customer details</strong> for&nbsp;
      <Link className="fc-activity__link" to="customer" params={{customerId: data.customerId}}>
        {data.oldInfo.name}
      </Link>.
    </span>
  );
};


export const details = data => {
  return {
    newOne: <CustomerInfo {...data.newInfo} />,
    previous: <CustomerInfo {...data.oldInfo} />,
  };
};
