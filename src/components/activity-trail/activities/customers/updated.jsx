
import React from 'react';
import { Link } from '../../../link';

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

const CustomerInfo = props => {
  return (
    <dl className="fc-activity__customer-info">
      <dt>Name</dt>
      <dd>{props.name}</dd>
      <dt>Email Address</dt>
      <dd>{props.email}</dd>
      <dt>Phome Number</dt>
      <dd>{props.phoneNumber}</dd>
    </dl>
  );
};

export const details = data => {
  return {
    newOne: <CustomerInfo {...data.newInfo} />,
    previous: <CustomerInfo {...data.oldInfo} />,
  };
};
