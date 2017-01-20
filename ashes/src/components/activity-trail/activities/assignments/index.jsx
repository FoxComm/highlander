// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';
import { assignmentTypes } from 'paragons/participants';

// components
import CordTarget from '../base/cord-target';
import CordLink from '../base/cord-link';
import Person from '../base/person';
import Title from '../base/title';

const bulkEventsToOrders = {
  title: (data, { kind }) => {
    const orders = data.entityIds.map(ref => <CordLink key={ref} cord={{referenceNumber: ref}} />);
    const directionSense = kind == types.BULK_ASSIGNED ? 'to' : 'from';
    let action = '';

    if (kind == types.BULK_ASSIGNED) {
      action = data.assignmentType == assignmentTypes.assignee ? 'assigned' : 'added watcher';
    } else {
      action = data.assignmentType == assignmentTypes.assignee ? 'unassigned' : 'removed watcher';
    }

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
      const persons = data.assignees.map((person, idx) => <Person key={idx} {...person} />);
      const action = data.assignmentType == assignmentTypes.assignee ? 'assigned' : 'added watcher';

      return (
        <Title activity={activity}>
          <strong>{action}</strong> {joinEntities(persons)} to <CordTarget cord={data.entity} />
        </Title>
      );
    },
  },
  [types.BULK_ASSIGNED]: bulkEventsToOrders,
  [types.BULK_UNASSIGNED]: bulkEventsToOrders,
  [types.UNASSIGNED]: {
    title: (data, activity) => {
      const action = data.assignmentType == assignmentTypes.assignee ? 'unassigned' : 'removed watcher';

      return (
        <Title activity={activity}>
          <strong>{action}</strong> <Person {...data.assignee} /> from <CordTarget cord={data.entity} />
        </Title>
      );
    }
  },
};

export default representatives;
