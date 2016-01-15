
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Person from '../base/person';

[
  types.ASSIGNED_TO_ORDER,
  types.UNASSIGNED_FROM_ORDER,
  types.BULK_ASSIGNED_TO_ORDERS,
  types.BULK_UNASSIGNED_FROM_ORDERS,
]

const representatives = {
  [types.ASSIGNED_TO_ORDER]: {
    title: data => {
      const persons = data.assignees.map(person => <Person {...person} />);

      return (
        <span>
          <strong>assigned</strong> {joinEntities(persons)} to <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.BULK_ASSIGNED_TO_ORDERS]: {
    title: data => {
      const orders = data.orders.map(referenceNumber => <OrderLink order={{title: 'Order', referenceNumber}} />);

      return (
        <span>
          <strong>assigned</strong> <Person {...data.assignee} /> to orders {joinEntities(orders)}.
        </span>
      );
    },
  },
  [types.UNASSIGNED_FROM_ORDER]: {
    title: data => {
      return (
        <span>
          <strong>unassigned</strong> <Person {...data.assignee} /> from <OrderTarget order={data.order} />.
        </span>
      );
    }
  },
};

export default representatives;
