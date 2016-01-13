
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

const customer = {
  id: 1,
  name: 'Thomas Angelo',
  email: 'angelo@salieri.it',
  phoneNumber: '+11111111111',
};

const activities = [
  // customers
  {
    kind: types.CUSTOMER_UPDATED,
    data: {
      customerId: 1,
      oldInfo: {
        name: 'William Shockley',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      },
      newInfo: {
        name: 'Shockley William',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      }
    }
  },
  {
    kind: types.CUSTOMER_CREATED,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REGISTERED,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ACTIVATED,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_BLACKLISTED,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REMOVED_FROM_BLACKLIST,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ENABLED,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_DISABLED,
    data: {
      customer
    }
  },


  // order shipping address

  {
    kind: types.ORDER_SHIPPING_ADDRESS_UPDATED,
    data: {
      order: {
        referenceNumber: 'BR10001',
        orderStatus: 'cart',
        shippingAddress: {
          "id": 3,
          "region": {
            "id": 4177,
            "countryId": 234,
            "name": "Washington"
          },
          "name": "Home",
          "address1": "555 E Lake Union St.",
          "city": "Seattle",
          "zip": "12345",
          "isDefault": false
        }
      },
      address: {
        "id": 3,
        "region": {
          "id": 4177,
          "countryId": 234,
          "name": "California"
        },
        "name": "South",
        "address1": "555 E Lake Union St.",
        "city": "Los Angeles",
        "zip": "54321",
        "isDefault": false
      }
    }
  },

  // order notes
  {
    kind: types.ORDER_NOTE_CREATED,
    data: {
      orderRefNum: 'BR10001',
      text: 'New note for order.'
    }
  },

  // orders
  {
    kind: types.ORDER_STATE_CHANGED,
    data: {
      order: {
        referenceNumber: 'BR10004',
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
