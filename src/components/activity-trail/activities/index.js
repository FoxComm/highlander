
import path from 'path';
import React, { PropTypes } from 'react';

import AddedNote from './added-note';
import ChangedOrderState from './changed-order-state';
import EditedShippingAddress from './edited-shipping-address';

export function getActivityRepresentative(activity) {
  switch (activity.type) {
    case 'added_note':
      return AddedNote;
    case 'changed_order_state':
      return ChangedOrderState;
    case 'edited_shipping_address':
      return EditedShippingAddress;
    default:
      return null;
  }
}

export default function(props) {
  const ActivityRepresentative = getActivityRepresentative(props.activity);

  if (!ActivityRepresentative) return null;

  return <ActivityRepresentative {...props} />;
}
