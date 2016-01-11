
import React from 'react';
import ActivityTrail from './activity-trail';
import types from './activities/base/types';
import OrderParagon from '../../paragons/order';

const activities = [
  {
    kind: types.ADDED_NOTE,
    target: "order",
    data: {
      order: new OrderParagon({
        referenceNumber: '123456789',
        orderStatus: 'cart'
      })
    }
  }
];

export default class AllActivities extends React.Component {

  render() {
    return (
      <div style={{margin: '20px'}}>
        <ActivityTrail activities={activities} hasMore={false} />;
      </div>
    );
  }
}
