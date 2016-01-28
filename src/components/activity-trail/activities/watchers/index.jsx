
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Person from '../base/person';

const bulkEventsToOrders = {
  title: (data, {kind}) => {
    const orders = data.orderRefNums.map(ref => <OrderLink key={ref} order={{title: 'Order', referenceNumber: ref}} />);
    const action = kind == types.BULK_ADDED_WATCHER_TO_ORDERS ? 'added watcher' : 'removed watcher';
    const directionSense = kind == types.BULK_ADDED_WATCHER_TO_ORDERS ? 'to' : 'from';

    return (
      <span>
        <strong>{action}</strong> <Person {...data.watcher} /> {directionSense} orders {joinEntities(orders)}.
      </span>
    );
  },
};

const representatives = {
  [types.ADDED_WATCHERS_TO_ORDER]: {
    title: data => {
      const persons = data.watchers.map((person, idx) => <Person key={idx} {...person} />);

      return (
        <span>
          <strong>added watchers</strong> {joinEntities(persons)} to <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.BULK_ADDED_WATCHER_TO_ORDERS]: bulkEventsToOrders,
  [types.BULK_REMOVED_WATCHER_FROM_ORDERS]: bulkEventsToOrders,
  [types.REMOVED_WATCHER_FROM_ORDER]: {
    title: data => {
      return (
        <span>
          <strong>removed watcher</strong> <Person {...data.watcher} /> from <OrderTarget order={data.order} />.
        </span>
      );
    }
  },
};

export default representatives;
