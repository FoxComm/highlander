
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Person from '../base/person';

const bulkProcessAssignToOrders = {
  title: (data, {kind}) => {
    const orders = data.orders.map(referenceNumber => <OrderLink order={{title: 'Order', referenceNumber}} />);
    const action = kind == types.BULK_ASSIGNED_TO_ORDERS ? 'assigned' : 'unassigned';
    const directionSense = kind == types.BULK_ASSIGNED_TO_ORDERS ? 'to' : 'from';

    return (
      <span>
        <strong>{action}</strong> <Person {...data.assignee} /> {directionSense} orders {joinEntities(orders)}.
      </span>
    );
  },
};

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
  [types.BULK_ASSIGNED_TO_ORDERS]: bulkProcessAssignToOrders,
  [types.BULK_UNASSIGNED_FROM_ORDERS]: bulkProcessAssignToOrders,
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
