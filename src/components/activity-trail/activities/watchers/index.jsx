
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import OrderTarget from '../base/order-target';
import OrderLink from '../base/order-link';
import Person from '../base/person';
import Title from '../base/title';

const bulkEventsToOrders = {
  title: (data, {kind}) => {
    const orders = data.orderRefNums.map(ref => <OrderLink key={ref} order={{title: 'Order', referenceNumber: ref}} />);
    const action = kind == types.BULK_ADDED_WATCHER ? 'added watcher' : 'removed watcher';
    const directionSense = kind == types.BULK_ADDED_WATCHER ? 'to' : 'from';

    return (
      <span>
        <strong>{action}</strong> <Person {...data.watcher} /> {directionSense} orders {joinEntities(orders)}.
      </span>
    );
  },
};

const representatives = {
  [types.ADDED_WATCHERS]: {
    title: (data, activity) => {
      const persons = data.watchers.map((person, idx) => <Person key={idx} {...person} />);

      return (
        <Title activity={activity}>
          <strong>added watchers</strong> {joinEntities(persons)} to <OrderTarget order={data.entity} />
        </Title>
      );
    },
  },
  [types.BULK_ADDED_WATCHER]: bulkEventsToOrders,
  [types.BULK_REMOVED_WATCHER]: bulkEventsToOrders,
  [types.REMOVED_WATCHER]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed watcher</strong> <Person {...data.watcher} /> from <OrderTarget order={data.entity} />
        </Title>
      );
    }
  },
};

export default representatives;
