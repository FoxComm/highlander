
import path from 'path';
import React, { PropTypes } from 'react';
import types from './base/types';

import AddedNote from './added-note';
import toggleLineItems from './toggled-line-items';
import ChangedOrderState from './changed-order-state';
import EditedShippingAddress from './edited-shipping-address';

export function getActivityRepresentative(activity) {
  switch (activity.type) {
    case types.ADDED_NOTE:
      return AddedNote;
    case types.CHANGED_ORDER_STATE:
      return ChangedOrderState;
    case types.EDITED_SHIPPING_ADDRESS:
      return EditedShippingAddress;
    case types.ADDED_LINE_ITEMS:
    case types.REMOVED_LINE_ITEMS:
      return toggleLineItems(activity.type);
    default:
      return null;
  }
}

export default function(props) {
  const ActivityRepresentative = getActivityRepresentative(props.activity);

  if (!ActivityRepresentative) return null;

  return <ActivityRepresentative {...props} />;
}
