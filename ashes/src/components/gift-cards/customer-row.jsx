/* @flow */

import React from 'react';

import { Checkbox } from 'components/core/checkbox';

type Props = {
  customer: Customer,
  checked?: boolean,
  onToggle?: (id: number) => void,
};

const CustomerRow = ({customer, checked = false, onToggle = (id) => {}}: Props) => {
  return (
    <li className="fc-choose-customers__entry" key={customer.id}>
      <Checkbox
        id={`choose-customers-${customer.id}`}
        label={customer.name}
        checked={checked}
        onChange={() => onToggle(customer.id)}
      />
      <div className="fc-choose-customers__info">
        <div className="fc-choose-customers__customer-email">
          {customer.email}
        </div>
        <div className="fc-choose-customers__customer-phone-number">
          {customer.phoneNumber}
        </div>
      </div>
    </li>
  );
};

export default CustomerRow;
