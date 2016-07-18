/* @flow */

import React from 'react';

import { Checkbox } from '../checkbox/checkbox';

type Props = {
  customer: {
    id: number,
    email: string,
    phoneNumber?: string,
    name: string,
  },
  checked: boolean,
  onToggle: (id: number) => void,
};

const CustomerRow = (props: Props) => {
  const { customer } = props;
  return (
    <li className="fc-choose-customers__entry" key={customer.id}>
      <Checkbox
        id={`choose-customers-${customer.id}`}
        checked={props.checked}
        onChange={() => props.onToggle(customer.id)}>
        {customer.name}
      </Checkbox>
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

CustomerRow.defaultProps = {
  checked: false,
};

export default CustomerRow;
