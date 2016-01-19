
import React from 'react';

import Activity from './base/activity';

import orderShippingAddressActivities from './order-shipping-address';
import orderNotesActivities from './order-notes';
import ordersActivities from './orders';
import customersActivities from './customers';

const representatives = {
  ...ordersActivities,
  ...orderNotesActivities,
  ...orderShippingAddressActivities,
  ...customersActivities,
};

export function getActivityRepresentative(activity) {
  const desc = representatives[activity.kind];

  if (!desc) return null;

  const args = [activity.data, activity];

  const params = {
    title: desc.title(...args),
    details: desc.details ? desc.details(...args) : null,
  };

  return props => <Activity {...params} {...props} />;
}

export default function(props) {
  const ActivityRepresentative = getActivityRepresentative(props.activity);

  if (!ActivityRepresentative) return null;

  return <ActivityRepresentative {...props} />;
}
