// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';
import { assignmentTypes } from '../../../../paragons/watcher';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Person from '../base/person';
import Title from '../base/title';

const bulkEventsToOrders = {
  title: (data, { kind }) => {
    const orders = data.orderRefNums.map(ref => <OrderLink key={ref} order={{title: 'Order', referenceNumber: ref}} />);
    const action = kind == types.BULK_ASSIGNED ? 'assigned' : 'unassigned';
    const directionSense = kind == types.BULK_ASSIGNED ? 'to' : 'from';

    return (
      <span>
        <strong>{action}</strong> <Person {...data.assignee} /> {directionSense} orders {joinEntities(orders)}.
      </span>
    );
  },
};

const representatives = {
  [types.ASSIGNED]: {
    title: (data, activity) => {
      const persons = Object.values(data.assignees).map((person, idx) => <Person key={idx} {...person} />);
      const action = data.assignmentType == assignmentTypes.assignee ? 'assigned' : 'added watcher';
      const order = { title: 'Order', referenceNumber: data.entity.referenceNumber };

      return (
        <Title activity={activity}>
          <strong>{action}</strong> {joinEntities(persons)} to <OrderTarget order={order} />
        </Title>
      );
    },
  },
  [types.BULK_ASSIGNED]: bulkEventsToOrders,
  [types.BULK_UNASSIGNED]: bulkEventsToOrders,
  [types.UNASSIGNED]: {
    title: (data, activity) => {
      const order = { title: 'Order', referenceNumber: data.entity.referenceNumber };
      const action = data.assignmentType == assignmentTypes.assignee ? 'unassigned' : 'removed watcher';

      return (
        <Title activity={activity}>
          <strong>{action}</strong> <Person {...data.assignee} /> from <OrderTarget order={order} />
        </Title>
      );
    }
  },
};

export default representatives;
