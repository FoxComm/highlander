
import React from 'react';

import { Link } from 'components/link';
import UserInfo from './user-info';

export const title = data => {
  return (
    <span>
      <strong>edited the customer details</strong> for&nbsp;
      <Link className="fc-activity__link" to="customer" params={{customerId: data.newInfo.id}}>
        {data.oldInfo.name}
      </Link>.
    </span>
  );
};


export const details = data => {
  return {
    newOne: <UserInfo {...data.newInfo} />,
    previous: <UserInfo {...data.oldInfo} />,
  };
};
