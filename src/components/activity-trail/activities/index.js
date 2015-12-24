
import React, { PropTypes } from 'react';
import types from './base/types';

import OrderTarget from './base/order-target';
import Activity from './base/activity';

import * as toggleLineItemsDesc from './toggled-line-items';
import * as editShippingAddressDesc from './edited-shipping-address';

const representatives = {
  [types.ADDED_NOTE]: {
    title: data => {
      return (
        <span>
          <strong>added a note</strong> on <OrderTarget order={data.order} />.
        </span>
      );
    }
  },
  [types.CHANGED_ORDER_STATE]: {
    title: data => {
      return (
        <span>
          <strong>changed the order state</strong> to {data.order.statusTitle}
          &nbsp;on <OrderTarget order={data.order} />.
        </span>
      );
    }
  },
  [types.EDITED_SHIPPING_ADDRESS]: editShippingAddressDesc,
  [types.ADDED_LINE_ITEMS]: toggleLineItemsDesc,
  [types.REMOVED_LINE_ITEMS]: toggleLineItemsDesc,
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
