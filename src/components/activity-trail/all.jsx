
import React from 'react';
import ActivityTrail from './activity-trail';
import types from './activities/base/types';
import { processActivity } from '../../modules/activity-trail';

function addContext(activity, i) {
  const userType = i % 2 ? 'system' : 'admin';

  activity.context = {userType};
  if (userType == 'admin') {
    activity.data.admin = {
      name: 'Jon Doe'
    };
  }

  return activity;
}

const activities = [
  {
    kind: types.ORDER_NOTE_CREATED,
    data: {
      orderRefNum: 'BR10001',
      text: 'New note for order.'
    }
  },
  {
    kind: types.ORDER_STATE_CHANGED,
    data: {
      order: {
        referenceNumber: 'BR10001',
        orderStatus: 'fraudHold'
      }
    }
  }
].map(processActivity).map(addContext);

export default class AllActivities extends React.Component {

  render() {
    return (
      <div style={{margin: '20px'}}>
        <ActivityTrail activities={activities} hasMore={false} />;
      </div>
    );
  }
}
