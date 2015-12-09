
import React, { PropTypes } from 'react';
import OrderTarget from './base/order-target';
import Activity from './base/activity';
import AddressDetails from '../../addresses/address-details';

export default function({activity}) {
  const { order } = activity.data;

  const actionDescription = (
    <div>
      {activity.data.author} <strong>edited the shipping address</strong> on <OrderTarget order={order} />.
    </div>
  );

  const removeName = address => {
    return {
      ...address,
      name: null
    };
  };

  const params = {
    activity,
    actionDescription,
    details: {
      newOne: <AddressDetails address={removeName(activity.data.newAddress)} />,
      previous: <AddressDetails address={removeName(activity.data.previousAddress)} />,
    },
    undoAction: () => 1,
  };

  return <Activity {...params} />;
}
